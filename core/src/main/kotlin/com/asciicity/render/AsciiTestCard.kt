package com.asciicity.render

import kotlin.math.roundToInt

object AsciiTestCard {
    private const val FAR_DEPTH = 20f
    private const val NEAR_DEPTH = 5f

    fun fill(frame: AsciiFrame, palette: RenderPalette) {
        val sky = palette[PaletteKeys.SKY]
        val fog = palette[PaletteKeys.FOG]
        val asphalt = palette[PaletteKeys.ASPHALT]
        frame.clear(' '.code, fog, sky)

        val horizon = (frame.height * 0.34f).roundToInt().coerceIn(6, frame.height - 9)
        drawGround(frame, palette, horizon)
        drawBuilding(frame, palette, 1, horizon, frame.width / 5, frame.height / 2, false)
        drawBuilding(frame, palette, frame.width / 5, horizon, frame.width / 7, frame.height / 3, true)
        drawBuilding(frame, palette, frame.width - frame.width / 4, horizon, frame.width / 4 - 1, frame.height * 2 / 3, false)
        drawBuilding(frame, palette, frame.width * 3 / 5, horizon, frame.width / 8, frame.height / 3, true)
        drawColorBands(frame, palette)

        for (x in 1 until frame.width - 1) {
            frame.set(x, horizon, '-'.code, fog, asphalt, FAR_DEPTH)
        }
        putText(frame, 3, horizon + 2, "NIGHT//MARKET", palette[PaletteKeys.NEON_MAGENTA], palette[PaletteKeys.WALL_FRONT])
        putText(
            frame,
            (frame.width - 15).coerceAtLeast(1),
            horizon + 5,
            "OPEN_24H",
            palette[PaletteKeys.NEON_CYAN],
            palette[PaletteKeys.WALL_SIDE],
        )

        drawBorder(frame, palette[PaletteKeys.NEON_CYAN], sky)
        val title = " ASCII CITY // GLYPH SURFACE ${frame.width}x${frame.height} "
        putText(frame, ((frame.width - title.length) / 2).coerceAtLeast(1), frame.height - 2, title, palette[PaletteKeys.HUD], sky)
        val ramp = "GLYPHS:  .:-=+*#%@  NEON: CYAN / MAGENTA / AMBER"
        putText(frame, ((frame.width - ramp.length) / 2).coerceAtLeast(1), 1, ramp, palette[PaletteKeys.NEON_AMBER], asphalt)
    }

    private fun drawGround(frame: AsciiFrame, palette: RenderPalette, horizon: Int) {
        val asphalt = palette[PaletteKeys.ASPHALT]
        val sidewalk = palette[PaletteKeys.SIDEWALK]
        val fog = palette[PaletteKeys.FOG]
        val roadColor = palette[PaletteKeys.WALL_FRONT]
        val center = frame.width / 2f

        for (y in 1 until horizon) {
            val distanceRatio = y.toFloat() / horizon.toFloat()
            val halfRoad = 5f + (frame.width * 0.31f - 5f) * (1f - distanceRatio)
            val leftEdge = (center - halfRoad).roundToInt()
            val rightEdge = (center + halfRoad).roundToInt()
            for (x in 1 until frame.width - 1) {
                val onRoad = x in leftEdge..rightEdge
                val background = if (onRoad) asphalt else sidewalk
                val glyph = if ((x + y * 3) % 17 == 0) '.'.code else ' '.code
                frame.set(x, y, glyph, fog, background, NEAR_DEPTH)
            }
            setIfInside(frame, leftEdge, y, '/'.code, roadColor, asphalt, NEAR_DEPTH)
            setIfInside(frame, rightEdge, y, '\\'.code, roadColor, asphalt, NEAR_DEPTH)
            if (y % 3 != 0) {
                setIfInside(frame, center.roundToInt(), y, '|'.code, palette[PaletteKeys.NEON_AMBER], asphalt, NEAR_DEPTH)
            }
        }
    }

    private fun drawBuilding(
        frame: AsciiFrame,
        palette: RenderPalette,
        startX: Int,
        baseY: Int,
        width: Int,
        height: Int,
        sideFace: Boolean,
    ) {
        val endX = (startX + width).coerceAtMost(frame.width - 1)
        val topY = (baseY + height).coerceAtMost(frame.height - 3)
        val wall = palette[if (sideFace) PaletteKeys.WALL_SIDE else PaletteKeys.WALL_FRONT]
        val edge = palette[PaletteKeys.FOG]
        for (y in baseY + 1..topY) {
            for (x in startX until endX) {
                val border = x == startX || x == endX - 1 || y == topY
                val window = !border && x % 4 == 0 && y % 3 == 0
                val glyph = when {
                    border -> '#'
                    window -> if ((x + y) % 8 == 0) '@' else '='
                    else -> ' '
                }
                val foreground = when {
                    border -> edge
                    window && (x + y) % 8 == 0 -> palette[PaletteKeys.WINDOW_LIT]
                    window -> palette[PaletteKeys.WINDOW_DARK]
                    else -> wall
                }
                frame.set(x, y, glyph.code, foreground, wall, FAR_DEPTH)
            }
        }
    }

    private fun drawBorder(frame: AsciiFrame, foreground: Int, background: Int) {
        for (x in 0 until frame.width) {
            frame.set(x, 0, if (x == 0 || x == frame.width - 1) '+'.code else '-'.code, foreground, background, 0f)
            frame.set(x, frame.height - 1, if (x == 0 || x == frame.width - 1) '+'.code else '-'.code, foreground, background, 0f)
        }
        for (y in 1 until frame.height - 1) {
            frame.set(0, y, '|'.code, foreground, background, 0f)
            frame.set(frame.width - 1, y, '|'.code, foreground, background, 0f)
        }
    }

    private fun drawColorBands(frame: AsciiFrame, palette: RenderPalette) {
        val colors = intArrayOf(
            palette[PaletteKeys.NEON_CYAN],
            palette[PaletteKeys.NEON_MAGENTA],
            palette[PaletteKeys.NEON_AMBER],
            palette[PaletteKeys.WINDOW_LIT],
        )
        val y = frame.height - 4
        var x = 3
        for (color in colors) {
            repeat(6) {
                setIfInside(frame, x, y, '='.code, color, palette[PaletteKeys.SKY], 0f)
                x += 1
            }
            x += 1
        }
    }

    private fun putText(frame: AsciiFrame, x: Int, y: Int, text: String, foreground: Int, background: Int) {
        for ((offset, character) in text.withIndex()) {
            setIfInside(frame, x + offset, y, character.code, foreground, background, 0f)
        }
    }

    private fun setIfInside(
        frame: AsciiFrame,
        x: Int,
        y: Int,
        glyph: Int,
        foreground: Int,
        background: Int,
        depth: Float,
    ) {
        if (x in 0 until frame.width && y in 0 until frame.height) {
            frame.set(x, y, glyph, foreground, background, depth)
        }
    }
}
