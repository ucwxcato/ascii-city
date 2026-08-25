package com.asciicity.world

data class SurfaceMaterial(
    val id: String,
    val glyphRamp: String,
    val paletteKey: String,
    val animation: SurfaceAnimation,
    val animationRate: Double,
    val phaseScale: Double,
    val tags: Set<String>,
    val accentPaletteKeys: List<String> = emptyList(),
)

enum class SurfaceAnimation {
    STATIC,
    DATA_STREAM,
    SIGNAL_PULSE,
    GLITCH_FLICKER,
}
