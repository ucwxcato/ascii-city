package com.asciicity.asset

data class AssetPaths(
    val font: String = DEFAULT_FONT,
    val denseWorldFont: String = DEFAULT_DENSE_WORLD_FONT,
    val fineWorldFont: String = DEFAULT_FINE_WORLD_FONT,
    val palette: String = DEFAULT_PALETTE,
) {
    init {
        require(font.isNotBlank()) { "Font path must not be blank" }
        require(denseWorldFont.isNotBlank()) { "Dense world font path must not be blank" }
        require(fineWorldFont.isNotBlank()) { "Fine world font path must not be blank" }
        require(palette.isNotBlank()) { "Palette path must not be blank" }
    }

    companion object {
        const val DEFAULT_FONT = "fonts/vt323-10x18.fnt"
        const val DEFAULT_DENSE_WORLD_FONT = "fonts/vt323-8x15.fnt"
        const val DEFAULT_FINE_WORLD_FONT = "fonts/vt323-6x12.fnt"
        const val DEFAULT_PALETTE = "data/palette.json"
    }
}
