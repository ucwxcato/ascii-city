package com.asciicity.render

import kotlin.math.floor
import kotlin.math.sin

class AtmosphericRenderer(
    emitters: Collection<AtmosphericEmitter>,
    private val materialSampler: MaterialSampler,
) {
    private val orderedEmitters = emitters.sortedBy(AtmosphericEmitter::id).toTypedArray()

    fun render(frame: AsciiFrame, pose: ViewPose, metrics: ProjectionMetrics, timeSeconds: Double): Int {
        var writes = 0
        for (emitter in orderedEmitters) {
            writes += renderFixture(frame, pose, metrics, emitter)
            val stablePhase = stableUnit(emitter.id)
            for (wispIndex in 0 until WISPS_PER_EMITTER) {
                val phase = fractional(
                    timeSeconds / emitter.periodSeconds + emitter.phaseOffset + stablePhase +
                        wispIndex.toDouble() / WISPS_PER_EMITTER,
                )
                val sway = sin((phase + stablePhase) * TAU)
                val crossDrift = emitter.drift * phase * sway
                val forwardDrift = emitter.drift * 0.35 * phase * sin((phase + stablePhase * 0.5) * TAU)
                val worldX = emitter.position.x + crossDrift
                val worldZ = emitter.position.z + forwardDrift
                val worldHeight = emitter.baseHeight + emitter.riseHeight * phase
                writes += renderWisp(
                    frame = frame,
                    pose = pose,
                    metrics = metrics,
                    worldX = worldX,
                    worldZ = worldZ,
                    worldHeight = worldHeight,
                    glyph = glyphAt(phase),
                )
            }
        }
        return writes
    }

    private fun renderWisp(
        frame: AsciiFrame,
        pose: ViewPose,
        metrics: ProjectionMetrics,
        worldX: Double,
        worldZ: Double,
        worldHeight: Double,
        glyph: Int,
    ): Int {
        val relativeX = worldX - pose.position.x
        val relativeZ = worldZ - pose.position.z
        val depth = relativeX * pose.forwardX + relativeZ * pose.forwardZ
        if (depth <= MINIMUM_DEPTH || depth > MAXIMUM_DRAW_DISTANCE || !depth.isFinite()) return 0
        val lateral = relativeX * pose.rightX + relativeZ * pose.rightZ
        val screenX = metrics.projectCameraPlaneX(lateral / depth)
        val screenY = metrics.projectWorldHeight(worldHeight, pose.eyeHeight, depth)
        val column = floor(screenX).toInt()
        val row = floor(screenY).toInt()
        var writes = writeProjectedGlyph(
            frame,
            column,
            row,
            glyph,
            PaletteKeys.WINDOW_LIT,
            depth,
        )
        if (depth <= MID_DISTANCE) {
            writes += writeProjectedGlyph(
                frame,
                column,
                row + 1,
                '~'.code,
                PaletteKeys.WINDOW_LIT,
                depth,
            )
        }
        if (depth <= NEAR_DISTANCE) {
            writes += writeProjectedGlyph(frame, column - 1, row, '('.code, PaletteKeys.WINDOW_LIT, depth)
            writes += writeProjectedGlyph(frame, column + 1, row, ')'.code, PaletteKeys.WINDOW_LIT, depth)
        }
        return writes
    }

    private fun renderFixture(
        frame: AsciiFrame,
        pose: ViewPose,
        metrics: ProjectionMetrics,
        emitter: AtmosphericEmitter,
    ): Int {
        val relativeX = emitter.position.x - pose.position.x
        val relativeZ = emitter.position.z - pose.position.z
        val depth = relativeX * pose.forwardX + relativeZ * pose.forwardZ
        if (depth <= MINIMUM_DEPTH || depth > MAXIMUM_DRAW_DISTANCE || !depth.isFinite()) return 0
        val lateral = relativeX * pose.rightX + relativeZ * pose.rightZ
        val screenX = metrics.projectCameraPlaneX(lateral / depth)
        val fixtureHeight = if (emitter.kind == AtmosphericEmitterKind.STEAM) {
            emitter.baseHeight
        } else {
            emitter.baseHeight + EXHAUST_STACK_HEIGHT
        }
        val screenY = metrics.projectWorldHeight(fixtureHeight, pose.eyeHeight, depth)
        val column = floor(screenX).toInt()
        val row = floor(screenY).toInt()
        val biasedDepth = (depth - FIXTURE_DEPTH_BIAS).coerceAtLeast(MINIMUM_DEPTH)
        var writes = if (emitter.kind == AtmosphericEmitterKind.STEAM) {
            writeProjectedGlyph(frame, column, row, '#'.code, PaletteKeys.WINDOW_LIT, biasedDepth)
        } else {
            writeProjectedGlyph(frame, column, row, '|'.code, PaletteKeys.WALL_FRONT, biasedDepth)
        }
        if (depth <= FIXTURE_DETAIL_DISTANCE) {
            if (emitter.kind == AtmosphericEmitterKind.STEAM) {
                writes += writeProjectedGlyph(frame, column - 2, row, '['.code, PaletteKeys.WINDOW_LIT, biasedDepth)
                writes += writeProjectedGlyph(frame, column - 1, row, '='.code, PaletteKeys.WINDOW_LIT, biasedDepth)
                writes += writeProjectedGlyph(frame, column, row, '#'.code, PaletteKeys.WINDOW_LIT, biasedDepth)
                writes += writeProjectedGlyph(frame, column + 1, row, '='.code, PaletteKeys.WINDOW_LIT, biasedDepth)
                writes += writeProjectedGlyph(frame, column + 2, row, ']'.code, PaletteKeys.WINDOW_LIT, biasedDepth)
            } else {
                writes += writeProjectedGlyph(frame, column - 1, row - 1, '['.code, PaletteKeys.WALL_FRONT, biasedDepth)
                writes += writeProjectedGlyph(frame, column, row - 1, '='.code, PaletteKeys.WALL_FRONT, biasedDepth)
                writes += writeProjectedGlyph(frame, column + 1, row - 1, ']'.code, PaletteKeys.WALL_FRONT, biasedDepth)
            }
        }
        return writes
    }

    private fun writeProjectedGlyph(
        frame: AsciiFrame,
        column: Int,
        row: Int,
        glyph: Int,
        paletteKey: String,
        depth: Double,
    ): Int {
        if (column !in 0 until frame.width || row !in 0 until frame.height) return 0
        return if (frame.writeIfNearer(
                x = column,
                y = row,
                glyph = glyph,
                foreground = materialSampler.foreground(paletteKey, side = null, depth = depth),
                background = frame.backgroundAt(column, row),
                depth = depth.toFloat(),
            )
        ) 1 else 0
    }

    private fun glyphAt(phase: Double): Int = when {
        phase < 0.30 -> 'o'.code
        phase < 0.68 -> 'O'.code
        else -> '@'.code
    }

    private fun stableUnit(id: String): Double {
        var hash = FNV_OFFSET_BASIS
        for (character in id) {
            hash = (hash xor character.code.toLong()) * FNV_PRIME
        }
        return (hash ushr 11).toDouble() / (1L shl 53).toDouble()
    }

    private fun fractional(value: Double): Double = value - floor(value)

    companion object {
        private const val WISPS_PER_EMITTER = 3
        private const val MINIMUM_DEPTH = 1e-6
        private const val MAXIMUM_DRAW_DISTANCE = 48.0
        private const val NEAR_DISTANCE = 18.0
        private const val MID_DISTANCE = 32.0
        private const val FIXTURE_DETAIL_DISTANCE = 24.0
        private const val FIXTURE_DEPTH_BIAS = 0.03
        private const val EXHAUST_STACK_HEIGHT = 0.25
        private const val TAU = Math.PI * 2.0
        private const val FNV_OFFSET_BASIS = -3750763034362895579L
        private const val FNV_PRIME = 1099511628211L
    }
}
