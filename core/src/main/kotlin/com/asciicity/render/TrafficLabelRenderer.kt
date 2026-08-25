package com.asciicity.render

import com.asciicity.world.WorldPosition
import kotlin.math.floor

/** Draws a compact wordmark over a moving traffic sprite without scaling its letters. */
internal class TrafficLabelRenderer {
    fun render(
        frame: AsciiFrame,
        pose: ViewPose,
        metrics: ProjectionMetrics,
        sprite: AsciiSprite,
        position: WorldPosition,
        label: String?,
    ): Int {
        if (label == null) return 0
        val relativeX = position.x - pose.position.x
        val relativeZ = position.z - pose.position.z
        val depth = relativeX * pose.forwardX + relativeZ * pose.forwardZ
        if (!depth.isFinite() || depth !in MINIMUM_DEPTH..MAXIMUM_DEPTH) return 0
        val lateral = relativeX * pose.rightX + relativeZ * pose.rightZ
        val centerColumn = floor(metrics.projectCameraPlaneX(lateral / depth)).toInt()
        val row = floor(metrics.projectWorldHeight(sprite.physicalHeight * LABEL_HEIGHT_FRACTION, pose.eyeHeight, depth)).toInt()
        if (row !in 0 until frame.height) return 0
        val firstColumn = centerColumn - label.length / 2
        val foreground = sprite.foregroundAt(0, 0)
        var writtenCells = 0
        for (index in label.indices) {
            val column = firstColumn + index
            val glyph = label[index]
            if (glyph == ' ' || column !in 0 until frame.width) continue
            if (frame.writeIfNearer(
                    column,
                    row,
                    glyph.code,
                    foreground,
                    frame.backgroundAt(column, row),
                    (depth - LABEL_DEPTH_BIAS).toFloat(),
                )
            ) {
                writtenCells += 1
            }
        }
        return writtenCells
    }

    private companion object {
        const val MINIMUM_DEPTH = 0.1
        const val MAXIMUM_DEPTH = 48.0
        const val LABEL_HEIGHT_FRACTION = 0.55
        const val LABEL_DEPTH_BIAS = 0.01
    }
}
