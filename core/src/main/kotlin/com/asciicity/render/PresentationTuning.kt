package com.asciicity.render

data class PresentationTuning(
    val fogStartDistance: Double,
    val fogEndDistance: Double,
    val fogEndGlyphDensity: Double,
    val nearContrastDistance: Double,
    val minimumNearLuminanceContrast: Double,
    val sideFaceBrightness: Double,
    val signalPulseMinimumBrightness: Double,
    val glitchBrightnessBoost: Double,
    val maximumGlitchRate: Double,
    val glitchGlyphChanceDivisor: Int,
    val glitchFlashChanceDivisor: Int,
) {
    init {
        require(fogStartDistance >= 0.0 && fogStartDistance.isFinite()) {
            "Fog start distance must be finite and non-negative"
        }
        require(fogEndDistance > fogStartDistance && fogEndDistance.isFinite()) {
            "Fog end distance must be finite and greater than fog start"
        }
        require(fogEndGlyphDensity.isFinite() && fogEndGlyphDensity in 0.0..1.0) {
            "Fog-end glyph density must be between zero and one"
        }
        require(nearContrastDistance >= 0.0 && nearContrastDistance <= fogStartDistance) {
            "Near-contrast distance must be between zero and fog start"
        }
        require(minimumNearLuminanceContrast.isFinite() && minimumNearLuminanceContrast in 0.0..1.0) {
            "Minimum near luminance contrast must be between zero and one"
        }
        require(sideFaceBrightness.isFinite() && sideFaceBrightness in 0.0..1.0) {
            "Side-face brightness must be between zero and one"
        }
        require(signalPulseMinimumBrightness.isFinite() && signalPulseMinimumBrightness in 0.0..1.0) {
            "Signal-pulse minimum brightness must be between zero and one"
        }
        require(glitchBrightnessBoost.isFinite() && glitchBrightnessBoost in 1.0..2.0) {
            "Glitch brightness boost must be between one and two"
        }
        require(maximumGlitchRate > 0.0 && maximumGlitchRate.isFinite()) {
            "Maximum glitch rate must be positive and finite"
        }
        require(glitchGlyphChanceDivisor >= 2) { "Glitch glyph chance divisor must be at least two" }
        require(glitchFlashChanceDivisor >= 2) { "Glitch flash chance divisor must be at least two" }
    }

    companion object {
        val DEFAULT = PresentationTuning(
            fogStartDistance = 10.0,
            fogEndDistance = 48.0,
            fogEndGlyphDensity = 0.38,
            nearContrastDistance = 8.0,
            minimumNearLuminanceContrast = 0.05,
            sideFaceBrightness = 0.66,
            signalPulseMinimumBrightness = 0.78,
            glitchBrightnessBoost = 1.15,
            maximumGlitchRate = 6.0,
            glitchGlyphChanceDivisor = 17,
            glitchFlashChanceDivisor = 29,
        )
    }
}
