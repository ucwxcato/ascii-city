package com.asciicity.render

/** Draws sparse, deterministic rain streaks only into uncovered sky cells. */
class RainRenderer(
    private val palette: RenderPalette,
) {
    fun render(frame: AsciiFrame, timeSeconds: Double) {
        for (column in 0 until frame.width) {
            val seed = hash(column)
            if ((seed and DENSITY_MASK) != 0) continue
            val length = MINIMUM_STREAK_LENGTH + ((seed ushr 4) and LENGTH_MASK)
            val speed = MINIMUM_SPEED + ((seed ushr 8) and SPEED_MASK) * SPEED_STEP
            val offset = (seed ushr 12) and OFFSET_MASK
            val cycle = frame.height + length
            // Frame rows increase upward because LibGDX's world-space origin is at the bottom left.
            // A falling head therefore moves toward smaller row values, with its trail above it.
            val head = positiveMod(offset - (timeSeconds * speed).toInt(), cycle) - length
            var streakOffset = 0
            while (streakOffset < length) {
                val row = head + streakOffset
                if (row in 0 until frame.height && frame.depthAt(column, row) == Float.POSITIVE_INFINITY) {
                    frame.set(
                        x = column,
                        y = row,
                        glyph = RAIN_GLYPHS[(seed + streakOffset) and RAIN_GLYPH_MASK].code,
                        foreground = palette[PaletteKeys.RAIN],
                        background = frame.backgroundAt(column, row),
                        depth = Float.POSITIVE_INFINITY,
                    )
                }
                streakOffset += 1
            }
        }
    }

    private fun hash(value: Int): Int {
        var result = value * 0x45D9F3B
        result = result xor (result ushr 16)
        result *= 0x45D9F3B
        return result xor (result ushr 16)
    }

    private fun positiveMod(value: Int, divisor: Int): Int {
        val result = value % divisor
        return if (result < 0) result + divisor else result
    }

    companion object {
        private const val DENSITY_MASK = 3
        private const val MINIMUM_STREAK_LENGTH = 2
        private const val LENGTH_MASK = 3
        private const val MINIMUM_SPEED = 7
        private const val SPEED_MASK = 3
        private const val SPEED_STEP = 2
        private const val OFFSET_MASK = 0xFF
        private const val RAIN_GLYPHS = "|:.'"
        private const val RAIN_GLYPH_MASK = 3
    }
}
