package com.asciicity.ambient

import com.asciicity.audio.AmbientAudioCue
import com.asciicity.render.AsciiFrame
import com.asciicity.render.MaterialSampler
import com.asciicity.render.PaletteKeys
import com.asciicity.render.ProjectionMetrics
import com.asciicity.render.ViewPose
import com.asciicity.world.WorldPosition
import kotlin.math.floor
import kotlin.math.hypot

data class AmbientEventPlayback(
    val cue: AmbientAudioCue,
    val volume: Float,
    val pan: Float,
    val pitch: Float,
)

class AmbientEventSystem(
    emitters: Collection<AmbientEventEmitter>,
) {
    private val states = emitters.sortedBy(AmbientEventEmitter::id).map(::State).toTypedArray()
    private var elapsedSeconds = 0.0

    fun step(deltaSeconds: Double, listener: ViewPose, play: (AmbientEventPlayback) -> Unit = {}) {
        require(deltaSeconds >= 0.0 && deltaSeconds.isFinite()) {
            "Ambient event delta must be finite and non-negative"
        }
        elapsedSeconds += deltaSeconds
        for (state in states) {
            state.activeSeconds = (state.activeSeconds - deltaSeconds).coerceAtLeast(0.0)
            if (elapsedSeconds + TIME_EPSILON < state.nextEventSeconds) continue
            state.activeSeconds = state.emitter.durationSeconds
            val mix = spatialMix(listener, state.emitter.position, state.emitter.audibleRange)
            if (mix.volume > 0f) {
                play(
                    AmbientEventPlayback(
                        cue = state.emitter.kind.audioCue,
                        volume = mix.volume,
                        pan = mix.pan,
                        pitch = pitchFor(state.emitter.id, state.eventIndex),
                    ),
                )
            }
            state.eventIndex += 1
            state.nextEventSeconds += intervalFor(state.emitter, state.eventIndex)
        }
    }

    fun render(
        frame: AsciiFrame,
        pose: ViewPose,
        metrics: ProjectionMetrics,
        materialSampler: MaterialSampler,
    ): Int {
        var writes = 0
        for (state in states) {
            if (state.activeSeconds <= 0.0) continue
            val relativeX = state.emitter.position.x - pose.position.x
            val relativeZ = state.emitter.position.z - pose.position.z
            val depth = relativeX * pose.forwardX + relativeZ * pose.forwardZ
            if (depth <= MINIMUM_DEPTH || depth > MAXIMUM_DRAW_DISTANCE || !depth.isFinite()) continue
            val lateral = relativeX * pose.rightX + relativeZ * pose.rightZ
            val screenX = metrics.projectCameraPlaneX(lateral / depth)
            val screenY = metrics.projectWorldHeight(state.emitter.height, pose.eyeHeight, depth)
            val column = floor(screenX).toInt()
            val row = floor(screenY).toInt()
            if (column !in 0 until frame.width || row !in 0 until frame.height) continue
            val glyphs = state.emitter.kind.glyphs
            val glyphIndex = floor(state.activeSeconds * 12.0).toInt().mod(glyphs.length)
            if (frame.writeIfNearer(
                    x = column,
                    y = row,
                    glyph = glyphs[glyphIndex].code,
                    foreground = materialSampler.foreground(eventPalette(state.emitter.kind), null, depth),
                    background = frame.backgroundAt(column, row),
                    depth = depth.toFloat(),
                )
            ) {
                writes += 1
            }
        }
        return writes
    }

    internal fun nextEventSeconds(index: Int): Double = states[index].nextEventSeconds

    internal fun isActive(index: Int): Boolean = states[index].activeSeconds > 0.0

    private fun spatialMix(listener: ViewPose, source: WorldPosition, audibleRange: Double): SpatialMix {
        val deltaX = source.x - listener.position.x
        val deltaZ = source.z - listener.position.z
        val distance = hypot(deltaX, deltaZ)
        if (distance >= audibleRange) return SpatialMix(0f, 0f)
        val normalizedDistance = (distance / audibleRange).coerceIn(0.0, 1.0)
        val volume = ((1.0 - normalizedDistance) * (1.0 - normalizedDistance)).toFloat()
        val pan = if (distance <= MINIMUM_DEPTH) 0f else {
            ((deltaX * listener.rightX + deltaZ * listener.rightZ) / distance).coerceIn(-1.0, 1.0).toFloat()
        }
        return SpatialMix(volume, pan)
    }

    private fun intervalFor(emitter: AmbientEventEmitter, eventIndex: Int): Double {
        val unit = stableUnit("${emitter.id}:$eventIndex")
        return emitter.minimumIntervalSeconds +
            (emitter.maximumIntervalSeconds - emitter.minimumIntervalSeconds) * unit
    }

    private fun pitchFor(id: String, eventIndex: Int): Float =
        (0.92 + stableUnit("$id:pitch:$eventIndex") * 0.16).toFloat()

    private fun stableUnit(value: String): Double {
        var hash = FNV_OFFSET_BASIS
        for (character in value) hash = (hash xor character.code.toLong()) * FNV_PRIME
        return (hash ushr 11).toDouble() / (1L shl 53).toDouble()
    }

    private fun eventPalette(kind: AmbientEventKind): String = when (kind) {
        AmbientEventKind.VENT_HISS -> PaletteKeys.FOG
        AmbientEventKind.ELECTRIC_SPARK -> PaletteKeys.SIGN_GOLD
        AmbientEventKind.DISTANT_HORN -> PaletteKeys.NEON_RED
    }

    private inner class State(val emitter: AmbientEventEmitter) {
        var eventIndex = 0
        var nextEventSeconds = intervalFor(emitter, eventIndex)
        var activeSeconds = 0.0
    }

    private data class SpatialMix(val volume: Float, val pan: Float)

    companion object {
        private const val TIME_EPSILON = 1e-9
        private const val MINIMUM_DEPTH = 1e-6
        private const val MAXIMUM_DRAW_DISTANCE = 48.0
        private const val FNV_OFFSET_BASIS = -3750763034362895579L
        private const val FNV_PRIME = 1099511628211L
    }
}
