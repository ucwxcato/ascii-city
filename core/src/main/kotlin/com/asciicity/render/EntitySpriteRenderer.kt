package com.asciicity.render

import com.asciicity.world.WorldPosition
import kotlin.math.ceil
import kotlin.math.floor

class EntitySpriteRenderer(
    private val maxDrawDistance: Double = DEFAULT_MAX_DRAW_DISTANCE,
) {
    init {
        require(maxDrawDistance > 0.0 && maxDrawDistance <= Float.MAX_VALUE && maxDrawDistance.isFinite()) {
            "Entity sprite draw distance must be positive, finite, and representable as frame depth"
        }
    }

    fun render(
        frame: AsciiFrame,
        pose: ViewPose,
        metrics: ProjectionMetrics,
        sprite: AsciiSprite,
        position: WorldPosition,
        anchorHeight: Double = 0.0,
    ): Int {
        require(metrics.columns == frame.width && metrics.rows == frame.height) {
            "Projection metrics must match the ASCII frame dimensions"
        }
        require(anchorHeight.isFinite()) { "Entity sprite anchor height must be finite" }

        val relativeX = position.x - pose.position.x
        val relativeZ = position.z - pose.position.z
        if (!relativeX.isFinite() || !relativeZ.isFinite()) return 0
        val depth = relativeX * pose.forwardX + relativeZ * pose.forwardZ
        if (!depth.isFinite() || depth <= MINIMUM_DEPTH || depth > maxDrawDistance) return 0
        val lateralCenter = relativeX * pose.rightX + relativeZ * pose.rightZ
        if (!lateralCenter.isFinite()) return 0

        val leftLateral = lateralCenter - sprite.anchorX * sprite.physicalWidth
        val rightLateral = leftLateral + sprite.physicalWidth
        val leftCameraPlane = leftLateral / depth
        val rightCameraPlane = rightLateral / depth
        if (!leftCameraPlane.isFinite() || !rightCameraPlane.isFinite()) return 0
        val leftScreen = metrics.projectCameraPlaneX(leftCameraPlane)
        val rightScreen = metrics.projectCameraPlaneX(rightCameraPlane)
        if (rightScreen <= 0.0 || leftScreen >= frame.width.toDouble()) return 0

        val bottomWorld = anchorHeight - sprite.anchorY * sprite.physicalHeight
        val topWorld = bottomWorld + sprite.physicalHeight
        val bottomScreen = metrics.projectWorldHeight(bottomWorld, pose.eyeHeight, depth)
        val topScreen = metrics.projectWorldHeight(topWorld, pose.eyeHeight, depth)
        if (topScreen <= 0.0 || bottomScreen >= frame.height.toDouble()) return 0

        val firstColumn = floor(leftScreen).toInt().coerceAtLeast(0)
        val lastColumn = (ceil(rightScreen).toInt() - 1).coerceAtMost(frame.width - 1)
        val firstRow = floor(bottomScreen).toInt().coerceAtLeast(0)
        val lastRow = (ceil(topScreen).toInt() - 1).coerceAtMost(frame.height - 1)
        if (firstColumn > lastColumn || firstRow > lastRow) return 0

        val spriteDepth = depth.toFloat()
        var writtenCells = 0
        var row = firstRow
        while (row <= lastRow) {
            val worldHeight = pose.eyeHeight +
                (row + 0.5 - metrics.horizonRow) * depth / metrics.focalRows
            val vertical = (worldHeight - bottomWorld) / sprite.physicalHeight
            if (vertical >= 0.0 && vertical < 1.0) {
                val sourceRow = sprite.rows - 1 - floor(vertical * sprite.rows).toInt()
                var column = firstColumn
                while (column <= lastColumn) {
                    val rayLateral = metrics.cameraPlaneXForColumn(column) * depth
                    val horizontal = (rayLateral - leftLateral) / sprite.physicalWidth
                    if (horizontal >= 0.0 && horizontal < 1.0) {
                        val sourceColumn = floor(horizontal * sprite.columns).toInt()
                        if (!sprite.isTransparent(sourceColumn, sourceRow) && frame.writeIfNearer(
                                x = column,
                                y = row,
                                glyph = sprite.glyphAt(sourceColumn, sourceRow),
                                foreground = sprite.foregroundAt(sourceColumn, sourceRow),
                                background = frame.backgroundAt(column, row),
                                depth = spriteDepth,
                            )
                        ) {
                            writtenCells += 1
                        }
                    }
                    column += 1
                }
            }
            row += 1
        }
        return writtenCells
    }

    companion object {
        const val DEFAULT_MAX_DRAW_DISTANCE: Double = 48.0
        private const val MINIMUM_DEPTH: Double = 1e-6
    }
}
