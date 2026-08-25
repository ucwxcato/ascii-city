package com.asciicity.render

enum class FacadeFace {
    NORTH,
    SOUTH,
    EAST,
    WEST,
    ;

    companion object {
        fun fromRayHit(side: RayHitSide, directionX: Double, directionZ: Double): FacadeFace? = when (side) {
            RayHitSide.X_FACE -> if (directionX > 0.0) WEST else EAST
            RayHitSide.Z_FACE -> if (directionZ > 0.0) SOUTH else NORTH
            RayHitSide.CORNER -> null
        }
    }
}

enum class FacadeLighting {
    MARQUEE,
    AMBER_PULSE,
    CLUB_STROBE,
}

data class FacadeSign(
    val id: String,
    val face: FacadeFace,
    val cellX: Int,
    val cellZ: Int,
    val widthCells: Int,
    val bottom: Double,
    val height: Double,
    val glyphRows: List<String>,
    val distantGlyphRows: List<String>,
    val borderPaletteKey: String,
    val textPaletteKey: String,
    val accentPaletteKey: String,
    val backgroundPaletteKey: String,
    val animationRate: Double,
    val lighting: FacadeLighting = FacadeLighting.MARQUEE,
    val label: String? = null,
) {
    init {
        require(id.isNotBlank()) { "Facade sign ID must not be blank" }
        require(cellX >= 0 && cellZ >= 0) { "Facade sign anchor must be non-negative" }
        require(widthCells > 0) { "Facade sign width must be positive" }
        require(bottom >= 0.0 && bottom.isFinite()) { "Facade sign bottom must be finite and non-negative" }
        require(height > 0.0 && height.isFinite()) { "Facade sign height must be positive and finite" }
        require(glyphRows.size >= 2) { "Facade sign must contain at least two glyph rows" }
        val columns = glyphRows.first().length
        require(columns >= 3) { "Facade sign rows must contain at least three glyphs" }
        require(glyphRows.all { it.length == columns }) { "Facade sign glyph rows must be rectangular" }
        require(glyphRows.all { row -> row.all { it.code in 32..126 } }) {
            "Facade sign glyph rows must contain printable ASCII"
        }
        require(distantGlyphRows.size >= 2) { "Facade sign must contain at least two distant glyph rows" }
        val distantColumns = distantGlyphRows.first().length
        require(distantColumns >= 3) { "Facade sign distant rows must contain at least three glyphs" }
        require(distantGlyphRows.all { it.length == distantColumns }) {
            "Facade sign distant glyph rows must be rectangular"
        }
        require(distantGlyphRows.all { row -> row.all { it.code in 32..126 } }) {
            "Facade sign distant glyph rows must contain printable ASCII"
        }
        require(borderPaletteKey.isNotBlank()) { "Facade sign border palette key must not be blank" }
        require(textPaletteKey.isNotBlank()) { "Facade sign text palette key must not be blank" }
        require(accentPaletteKey.isNotBlank()) { "Facade sign accent palette key must not be blank" }
        require(backgroundPaletteKey.isNotBlank()) { "Facade sign background palette key must not be blank" }
        require(animationRate in 0.0..MAXIMUM_ANIMATION_RATE && animationRate.isFinite()) {
            "Facade sign animation rate must be finite and between zero and $MAXIMUM_ANIMATION_RATE"
        }
        require(label == null || label.isNotBlank() && label.length <= MAXIMUM_LABEL_LENGTH && label.all { it.code in 32..126 }) {
            "Facade sign label must be non-blank printable ASCII and at most $MAXIMUM_LABEL_LENGTH glyphs"
        }
    }

    internal fun glyphRowsAt(depth: Double): List<String> =
        if (depth >= DISTANT_LOD_DEPTH) distantGlyphRows else glyphRows

    internal fun cellXAt(offset: Int): Int = if (face == FacadeFace.NORTH || face == FacadeFace.SOUTH) {
        cellX + offset
    } else {
        cellX
    }

    internal fun cellZAt(offset: Int): Int = if (face == FacadeFace.EAST || face == FacadeFace.WEST) {
        cellZ + offset
    } else {
        cellZ
    }

    companion object {
        const val MAXIMUM_ANIMATION_RATE = 4.0
        const val MAXIMUM_LABEL_LENGTH = 32
        private const val DISTANT_LOD_DEPTH = 14.0
    }
}
