package com.asciicity.render

import kotlin.math.floor
import kotlin.math.hypot

/** Draws readable labels from points on their authored facade plane. */
internal class FacadeLabelRenderer(
    signs: Collection<FacadeSign>,
    private val materialSampler: MaterialSampler,
) {
    private val labeledSigns = signs.filter { it.label != null }.toTypedArray()

    fun render(frame: AsciiFrame, pose: ViewPose, metrics: ProjectionMetrics): Int {
        var writtenCells = 0
        for (sign in labeledSigns) {
            writtenCells += renderLabel(frame, pose, metrics, sign)
        }
        return writtenCells
    }

    private fun renderLabel(frame: AsciiFrame, pose: ViewPose, metrics: ProjectionMetrics, sign: FacadeSign): Int {
        val surfaceX = when (sign.face) {
            FacadeFace.NORTH,
            FacadeFace.SOUTH -> sign.cellX + sign.widthCells * 0.5
            FacadeFace.EAST -> sign.cellX + 1.0
            FacadeFace.WEST -> sign.cellX.toDouble()
        }
        val surfaceZ = when (sign.face) {
            FacadeFace.NORTH -> sign.cellZ + 1.0
            FacadeFace.SOUTH -> sign.cellZ.toDouble()
            FacadeFace.EAST,
            FacadeFace.WEST -> sign.cellZ + sign.widthCells * 0.5
        }
        val normalX = when (sign.face) {
            FacadeFace.EAST -> 1.0
            FacadeFace.WEST -> -1.0
            else -> 0.0
        }
        val normalZ = when (sign.face) {
            FacadeFace.NORTH -> 1.0
            FacadeFace.SOUTH -> -1.0
            else -> 0.0
        }
        val playerOffsetX = pose.position.x - surfaceX
        val playerOffsetZ = pose.position.z - surfaceZ
        val playerDistance = hypot(playerOffsetX, playerOffsetZ)
        val facingAlignment = (playerOffsetX * normalX + playerOffsetZ * normalZ) / playerDistance
        if (!facingAlignment.isFinite() || facingAlignment < MINIMUM_FACE_ALIGNMENT) return 0
        val label = checkNotNull(sign.label)
        // The label belongs in the center band of the marquee, not at street level
        // beneath it. Players look up at a tall sign to read its attached wording.
        val labelHeight = sign.bottom + sign.height * LABEL_HEIGHT_FRACTION
        val tangentX = when (sign.face) {
            FacadeFace.NORTH -> -1.0
            FacadeFace.SOUTH -> 1.0
            else -> 0.0
        }
        val tangentZ = when (sign.face) {
            FacadeFace.EAST -> 1.0
            FacadeFace.WEST -> -1.0
            else -> 0.0
        }
        val characterAdvance = minOf(
            BASE_CHARACTER_ADVANCE,
            sign.widthCells * MAXIMUM_LABEL_WIDTH_FRACTION / label.length,
        )
        val firstOffset = -(label.length - 1) * characterAdvance * 0.5
        var writtenCells = 0
        for (index in label.indices) {
            val glyph = label[index]
            if (glyph == ' ') continue
            val offset = firstOffset + index * characterAdvance
            val letterX = surfaceX + normalX * LABEL_SURFACE_OFFSET + tangentX * offset
            val letterZ = surfaceZ + normalZ * LABEL_SURFACE_OFFSET + tangentZ * offset
            val relativeX = letterX - pose.position.x
            val relativeZ = letterZ - pose.position.z
            val depth = relativeX * pose.forwardX + relativeZ * pose.forwardZ
            if (!depth.isFinite() || depth !in MINIMUM_DEPTH..MAXIMUM_LABEL_DEPTH) continue
            val lateral = relativeX * pose.rightX + relativeZ * pose.rightZ
            val column = floor(metrics.projectCameraPlaneX(lateral / depth)).toInt()
            val row = floor(metrics.projectWorldHeight(labelHeight, pose.eyeHeight, depth)).toInt()
            if (column !in 0 until frame.width || row !in 0 until frame.height) continue
            val foreground = materialSampler.foreground(sign.textPaletteKey, side = null, depth = depth)
            val background = materialSampler.background(sign.backgroundPaletteKey, side = null)
            if (frame.writeIfNearer(
                    column,
                    row,
                    glyph.code,
                    foreground,
                    background,
                    (depth - LABEL_DEPTH_BIAS).toFloat(),
                    emissive = LABEL_EMISSIVE_STRENGTH,
                )
            ) {
                writtenCells += 1
            }
        }
        return writtenCells
    }

    private companion object {
        const val MINIMUM_DEPTH = 0.1
        const val MAXIMUM_LABEL_DEPTH = 40.0
        const val MINIMUM_FACE_ALIGNMENT = 0.75
        const val LABEL_HEIGHT_FRACTION = 0.55
        const val BASE_CHARACTER_ADVANCE = 0.14
        const val MAXIMUM_LABEL_WIDTH_FRACTION = 0.7
        const val LABEL_SURFACE_OFFSET = 0.01
        const val LABEL_DEPTH_BIAS = 0.01
        const val LABEL_EMISSIVE_STRENGTH = 0.9f
    }
}
