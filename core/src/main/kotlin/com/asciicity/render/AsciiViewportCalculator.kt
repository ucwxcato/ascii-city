package com.asciicity.render

data class AsciiViewport(
    val columns: Int,
    val rows: Int,
    val glyphScale: Int,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val gutterX: Int,
    val gutterY: Int,
    val profile: AsciiFontProfile = AsciiFontProfile.STANDARD,
) {
    init {
        require(columns > 0 && rows > 0) { "ASCII viewport dimensions must be positive" }
        require(glyphScale > 0) { "ASCII viewport glyph scale must be positive" }
        require(pixelWidth == columns * profile.cellWidth * glyphScale) {
            "ASCII viewport pixel width must match its columns and glyph scale"
        }
        require(pixelHeight == rows * profile.cellHeight * glyphScale) {
            "ASCII viewport pixel height must match its rows and glyph scale"
        }
        require(gutterX >= 0 && gutterY >= 0) { "ASCII viewport gutters cannot be negative" }
    }

    val scaledCellWidth: Int = profile.cellWidth * glyphScale
    val scaledCellHeight: Int = profile.cellHeight * glyphScale
}

object AsciiViewportCalculator {
    const val MINIMUM_LOGICAL_WIDTH = FontMetrics.MINIMUM_COLUMNS * FontMetrics.CELL_WIDTH
    const val MINIMUM_LOGICAL_HEIGHT = FontMetrics.MINIMUM_ROWS * FontMetrics.CELL_HEIGHT
    const val REFERENCE_LOGICAL_WIDTH = FontMetrics.REFERENCE_COLUMNS * FontMetrics.CELL_WIDTH
    const val REFERENCE_LOGICAL_HEIGHT = FontMetrics.REFERENCE_ROWS * FontMetrics.CELL_HEIGHT

    fun calculate(
        logicalWidth: Int,
        logicalHeight: Int,
        drawableWidth: Int = logicalWidth,
        drawableHeight: Int = logicalHeight,
        profile: AsciiFontProfile = AsciiFontProfile.STANDARD,
    ): AsciiViewport {
        require(logicalWidth > 0 && logicalHeight > 0) { "Logical viewport dimensions must be positive" }
        require(drawableWidth > 0 && drawableHeight > 0) { "Drawable viewport dimensions must be positive" }

        val boundedWidth = logicalWidth.coerceAtLeast(profile.minimumLogicalWidth)
        val boundedHeight = logicalHeight.coerceAtLeast(profile.minimumLogicalHeight)
        val referenceScale = minOf(
            scaleWithinDesktopWorkArea(boundedWidth, profile.referenceLogicalWidth),
            scaleWithinDesktopWorkArea(boundedHeight, profile.referenceLogicalHeight),
        )
        val maximumScaleWithMinimumSurface = minOf(
            boundedWidth / profile.minimumLogicalWidth,
            boundedHeight / profile.minimumLogicalHeight,
        )
        val glyphScale = minOf(referenceScale, maximumScaleWithMinimumSurface).coerceAtLeast(1)
        val scaledCellWidth = profile.cellWidth * glyphScale
        val scaledCellHeight = profile.cellHeight * glyphScale
        val columns = boundedWidth / scaledCellWidth
        val rows = boundedHeight / scaledCellHeight
        val pixelWidth = columns * scaledCellWidth
        val pixelHeight = rows * scaledCellHeight

        return AsciiViewport(
            columns = columns,
            rows = rows,
            glyphScale = glyphScale,
            pixelWidth = pixelWidth,
            pixelHeight = pixelHeight,
            gutterX = (boundedWidth - pixelWidth) / 2,
            gutterY = (boundedHeight - pixelHeight) / 2,
            profile = profile,
        )
    }

    private fun scaleWithinDesktopWorkArea(size: Int, referenceSize: Int): Int =
        ((size.toLong() * PERCENT) / (referenceSize.toLong() * WORK_AREA_PERCENT)).toInt()

    private const val PERCENT = 100L
    private const val WORK_AREA_PERCENT = 95L
}
