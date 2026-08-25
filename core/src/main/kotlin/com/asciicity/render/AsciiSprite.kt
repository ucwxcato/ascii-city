package com.asciicity.render

class AsciiSprite internal constructor(
    val id: String,
    val columns: Int,
    val rows: Int,
    val anchorX: Double,
    val anchorY: Double,
    val physicalWidth: Double,
    val physicalHeight: Double,
    glyphs: IntArray,
    foregroundColors: IntArray,
) {
    private val glyphs = glyphs.copyOf()
    private val foregroundColors = foregroundColors.copyOf()

    init {
        require(id.isNotBlank()) { "ASCII sprite ID must not be blank" }
        require(columns in 1..MAXIMUM_COLUMNS) {
            "ASCII sprite columns must be between 1 and $MAXIMUM_COLUMNS"
        }
        require(rows in 1..MAXIMUM_ROWS) {
            "ASCII sprite rows must be between 1 and $MAXIMUM_ROWS"
        }
        require(anchorX in 0.0..1.0 && anchorX.isFinite()) {
            "ASCII sprite anchor x must be finite and between zero and one"
        }
        require(anchorY in 0.0..1.0 && anchorY.isFinite()) {
            "ASCII sprite anchor y must be finite and between zero and one"
        }
        require(physicalWidth > 0.0 && physicalWidth <= MAXIMUM_PHYSICAL_SIZE && physicalWidth.isFinite()) {
            "ASCII sprite physical width must be finite, positive, and at most $MAXIMUM_PHYSICAL_SIZE"
        }
        require(physicalHeight > 0.0 && physicalHeight <= MAXIMUM_PHYSICAL_SIZE && physicalHeight.isFinite()) {
            "ASCII sprite physical height must be finite, positive, and at most $MAXIMUM_PHYSICAL_SIZE"
        }
        val cellCount = Math.multiplyExact(columns, rows)
        require(glyphs.size == cellCount) { "ASCII sprite glyph data must contain $cellCount cells" }
        require(foregroundColors.size == cellCount) { "ASCII sprite color data must contain $cellCount cells" }
        require(glyphs.any { it != TRANSPARENT_GLYPH }) { "ASCII sprite must contain at least one visible glyph" }
    }

    fun glyphAt(column: Int, rowFromTop: Int): Int = glyphs[indexOf(column, rowFromTop)]

    fun foregroundAt(column: Int, rowFromTop: Int): Int = foregroundColors[indexOf(column, rowFromTop)]

    fun isTransparent(column: Int, rowFromTop: Int): Boolean =
        glyphAt(column, rowFromTop) == TRANSPARENT_GLYPH

    private fun indexOf(column: Int, rowFromTop: Int): Int {
        require(column in 0 until columns) { "ASCII sprite column $column is outside 0 until $columns" }
        require(rowFromTop in 0 until rows) { "ASCII sprite row $rowFromTop is outside 0 until $rows" }
        return rowFromTop * columns + column
    }

    companion object {
        const val TRANSPARENT_GLYPH: Int = ' '.code
        const val MAXIMUM_COLUMNS: Int = 64
        const val MAXIMUM_ROWS: Int = 64
        const val MAXIMUM_PHYSICAL_SIZE: Double = 16.0
    }
}
