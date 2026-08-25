package com.asciicity.render

import kotlin.math.floor

internal class FacadeSignSampler(
    signs: Collection<FacadeSign>,
    private val materialSampler: MaterialSampler,
    private val gridWidth: Int,
    private val gridDepth: Int,
) {
    private val signsByCellFace = arrayOfNulls<MutableList<FacadeSign>>(gridWidth * gridDepth * FacadeFace.entries.size)

    init {
        require(gridWidth > 0 && gridDepth > 0) { "Facade sign grid dimensions must be positive" }
        for (sign in signs) {
            var offset = 0
            while (offset < sign.widthCells) {
                val x = sign.cellXAt(offset)
                val z = sign.cellZAt(offset)
                require(x in 0 until gridWidth && z in 0 until gridDepth) {
                    "Facade sign '${sign.id}' extends outside the world grid"
                }
                val index = indexOf(x, z, sign.face)
                val overlays = signsByCellFace[index] ?: mutableListOf<FacadeSign>().also { signsByCellFace[index] = it }
                overlays += sign
                offset += 1
            }
        }
    }

    fun sample(
        cellX: Int,
        cellZ: Int,
        face: FacadeFace,
        side: RayHitSide,
        faceCoordinate: Double,
        vertical: Double,
        depth: Double,
        timeSeconds: Double,
        output: FacadeSignSample,
    ): Boolean {
        val overlays = signsByCellFace[indexOf(cellX, cellZ, face)] ?: return false
        var sign: FacadeSign? = null
        var overlayIndex = 0
        while (overlayIndex < overlays.size) {
            val candidate = overlays[overlayIndex]
            if (vertical >= candidate.bottom && vertical < candidate.bottom + candidate.height) {
                sign = candidate
                break
            }
            overlayIndex += 1
        }
        sign ?: return false

        val segment = if (face == FacadeFace.NORTH || face == FacadeFace.SOUTH) {
            cellX - sign.cellX
        } else {
            cellZ - sign.cellZ
        }
        val rawHorizontal = (segment + faceCoordinate) / sign.widthCells
        val readableHorizontal = if (face == FacadeFace.NORTH || face == FacadeFace.WEST) {
            1.0 - rawHorizontal
        } else {
            rawHorizontal
        }
        val glyphRows = sign.glyphRowsAt(depth)
        val glyphColumns = glyphRows.first().length
        val glyphColumn = floor(readableHorizontal * glyphColumns)
            .toInt().coerceIn(0, glyphColumns - 1)
        val fromTop = (sign.bottom + sign.height - vertical) / sign.height
        val glyphRow = floor(fromTop * glyphRows.size)
            .toInt().coerceIn(0, glyphRows.lastIndex)
        val authoredGlyph = glyphRows[glyphRow][glyphColumn]
        val animationTick = floor(timeSeconds * sign.animationRate).toInt()
        val animatedBulb = authoredGlyph == '.' && positiveMod(glyphColumn + glyphRow + animationTick, BULB_PERIOD) == 0
        val entranceLight = authoredGlyph in ENTRANCE_LIGHT_GLYPHS
        val clubLight = sign.lighting == FacadeLighting.CLUB_STROBE && entranceLight
        val amberLight = sign.lighting == FacadeLighting.AMBER_PULSE && entranceLight
        val pulseOn = positiveMod(animationTick + glyphColumn + glyphRow, PULSE_PERIOD) < PULSE_ON_TICKS
        val clubPalette = when (positiveMod(animationTick + glyphColumn + glyphRow, CLUB_PALETTE_COUNT)) {
            0 -> sign.accentPaletteKey
            1 -> sign.textPaletteKey
            else -> sign.borderPaletteKey
        }
        output.glyph = if (animatedBulb) 'o'.code else authoredGlyph.code
        val paletteKey = when {
            clubLight -> clubPalette
            amberLight && pulseOn -> sign.accentPaletteKey
            amberLight -> sign.backgroundPaletteKey
            authoredGlyph == '.' && animatedBulb -> sign.accentPaletteKey
            authoredGlyph == '.' || authoredGlyph in BORDER_GLYPHS -> sign.borderPaletteKey
            authoredGlyph.isDigit() || authoredGlyph in ACCENT_GLYPHS -> sign.accentPaletteKey
            else -> sign.textPaletteKey
        }
        output.foreground = materialSampler.foreground(paletteKey, side, depth)
        output.background = materialSampler.background(
            when {
                clubLight -> clubPalette
                amberLight && pulseOn -> sign.accentPaletteKey
                else -> sign.backgroundPaletteKey
            },
            side,
        )
        return true
    }

    private fun indexOf(x: Int, z: Int, face: FacadeFace): Int =
        (z * gridWidth + x) * FacadeFace.entries.size + face.ordinal

    private fun positiveMod(value: Int, divisor: Int): Int {
        val result = value % divisor
        return if (result < 0) result + divisor else result
    }

    companion object {
        private const val BULB_PERIOD = 4
        private const val PULSE_PERIOD = 4
        private const val PULSE_ON_TICKS = 2
        private const val CLUB_PALETTE_COUNT = 3
        private const val BORDER_GLYPHS = "+-|="
        private const val ACCENT_GLYPHS = "/\\!?#*<>"
        private const val ENTRANCE_LIGHT_GLYPHS = "@~"
    }
}

internal class FacadeSignSample {
    var glyph: Int = ' '.code
    var foreground: Int = 0
    var background: Int = 0
}
