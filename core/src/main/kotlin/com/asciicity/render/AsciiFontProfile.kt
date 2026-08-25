package com.asciicity.render

import com.badlogic.gdx.graphics.g2d.BitmapFont
import kotlin.math.roundToInt

/**
 * Immutable metrics for the CPU-rendered world surface. UI text deliberately keeps its own
 * readable font contract and does not inherit this profile.
 */
enum class AsciiFontProfile(
    val id: String,
    val fontPath: String,
    val cellWidth: Int,
    val cellHeight: Int,
    val baselineFromCellBottom: Int,
    val minimumColumns: Int,
    val minimumRows: Int,
    val referenceColumns: Int,
    val referenceRows: Int,
) {
    STANDARD(
        id = "standard",
        fontPath = "fonts/vt323-10x18.fnt",
        cellWidth = 10,
        cellHeight = 18,
        baselineFromCellBottom = 18,
        minimumColumns = 96,
        minimumRows = 30,
        referenceColumns = 128,
        referenceRows = 40,
    ),
    DENSE(
        id = "dense",
        fontPath = "fonts/vt323-8x15.fnt",
        cellWidth = 8,
        cellHeight = 15,
        baselineFromCellBottom = 15,
        minimumColumns = 120,
        minimumRows = 36,
        referenceColumns = 160,
        referenceRows = 48,
    ),
    FINE(
        id = "fine",
        fontPath = "fonts/vt323-6x12.fnt",
        cellWidth = 6,
        cellHeight = 12,
        baselineFromCellBottom = 12,
        minimumColumns = 160,
        minimumRows = 45,
        referenceColumns = 213,
        referenceRows = 60,
    ),
    ;

    init {
        require(cellWidth > 0 && cellHeight > 0) { "ASCII profile '$id' cell dimensions must be positive" }
        require(baselineFromCellBottom in 1..cellHeight) {
            "ASCII profile '$id' baseline must fit inside its cell"
        }
        require(minimumColumns > 0 && minimumRows > 0) { "ASCII profile '$id' minimum surface must be positive" }
        require(referenceColumns >= minimumColumns && referenceRows >= minimumRows) {
            "ASCII profile '$id' reference surface must include its minimum surface"
        }
    }

    val minimumLogicalWidth: Int
        get() = minimumColumns * cellWidth
    val minimumLogicalHeight: Int
        get() = minimumRows * cellHeight
    val referenceLogicalWidth: Int
        get() = referenceColumns * cellWidth
    val referenceLogicalHeight: Int
        get() = referenceRows * cellHeight

    fun validate(font: BitmapFont) {
        require(font.data.lineHeight.roundToInt() == cellHeight) {
            "Bitmap font for '$id' line height must be $cellHeight, but was ${font.data.lineHeight}"
        }
        for (codePoint in PRINTABLE_ASCII) {
            val glyph = font.data.getGlyph(codePoint.toChar())
                ?: throw IllegalArgumentException("Bitmap font for '$id' is missing printable ASCII code point $codePoint")
            require(glyph.width == cellWidth) {
                "Glyph '$codePoint' for '$id' width must be $cellWidth, but was ${glyph.width}"
            }
            require(glyph.height == cellHeight) {
                "Glyph '$codePoint' for '$id' height must be $cellHeight, but was ${glyph.height}"
            }
            require(glyph.xadvance == cellWidth) {
                "Glyph '$codePoint' for '$id' advance must be $cellWidth, but was ${glyph.xadvance}"
            }
        }
    }

    fun validateDefinition(definition: String) {
        val commonLine = definition.lineSequence().firstOrNull { it.startsWith("common ") }
            ?: throw IllegalArgumentException("Bitmap font definition for '$id' is missing the common line")
        require(integerField(commonLine, "lineHeight") == cellHeight) {
            "Bitmap font definition for '$id' lineHeight must be $cellHeight"
        }

        val glyphLines = definition.lineSequence().filter { it.startsWith("char ") }.toList()
        val glyphsByCodePoint = glyphLines.associateBy { integerField(it, "id") }
        require(glyphsByCodePoint.keys == PRINTABLE_ASCII) {
            "Bitmap font definition for '$id' must contain every printable ASCII code point from 32 through 126"
        }
        for ((codePoint, line) in glyphsByCodePoint) {
            require(integerField(line, "width") == cellWidth) { "Glyph '$codePoint' for '$id' width must be $cellWidth" }
            require(integerField(line, "height") == cellHeight) { "Glyph '$codePoint' for '$id' height must be $cellHeight" }
            require(integerField(line, "xadvance") == cellWidth) { "Glyph '$codePoint' for '$id' advance must be $cellWidth" }
        }
    }

    companion object {
        /** The desktop game's default world surface; UI retains the Standard font. */
        val DEFAULT: AsciiFontProfile = FINE

        private val PRINTABLE_ASCII = (32..126).toSet()

        fun fromId(id: String): AsciiFontProfile = entries.firstOrNull { it.id == id.lowercase() }
            ?: throw IllegalArgumentException("--ascii-density must be one of: ${entries.joinToString { it.id }}")

        private fun integerField(line: String, name: String): Int {
            val match = Regex("(?:^|\\s)$name=(-?\\d+)(?:\\s|$)").find(line)
                ?: throw IllegalArgumentException("Bitmap font line is missing integer field '$name': $line")
            return match.groupValues[1].toInt()
        }
    }
}
