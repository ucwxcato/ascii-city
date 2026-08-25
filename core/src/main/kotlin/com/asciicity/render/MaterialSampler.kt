package com.asciicity.render

import com.asciicity.world.SurfaceAnimation
import com.asciicity.world.SurfaceMaterial
import kotlin.math.floor
import kotlin.math.sin

class MaterialSampler(
    private val palette: RenderPalette,
    private val tuning: PresentationTuning = PresentationTuning.DEFAULT,
) {
    fun glyph(
        material: SurfaceMaterial,
        worldX: Double,
        worldZ: Double,
        vertical: Double,
        timeSeconds: Double,
        depth: Double = 0.0,
    ): Int {
        val baseHash = stableHash(
            floor(worldX * 7.0).toInt(),
            floor(worldZ * 7.0).toInt(),
            floor(vertical * 3.0).toInt(),
            material.id.hashCode(),
        )
        val tileGlyph = structuralGlyph(material, worldX, worldZ, vertical, depth)
        if (tileGlyph != null) {
            return if (keepGlyphAtDepth(baseHash, material, depth)) tileGlyph else ' '.code
        }
        val phase = (worldX + worldZ * 1.37 + vertical * 0.73) * material.phaseScale
        val index = when (material.animation) {
            SurfaceAnimation.STATIC -> positiveMod(baseHash, material.glyphRamp.length)
            SurfaceAnimation.DATA_STREAM -> floor(phase + timeSeconds * material.animationRate).toInt()
            SurfaceAnimation.SIGNAL_PULSE -> {
                val pulse = (sin(phase + timeSeconds * material.animationRate) * 0.5 + 0.5)
                floor(pulse * material.glyphRamp.length).toInt()
            }
            SurfaceAnimation.GLITCH_FLICKER -> {
                val tick = floor(timeSeconds * minOf(material.animationRate, tuning.maximumGlitchRate)).toInt()
                val glitchHash = stableHash(baseHash, tick, material.id.hashCode(), 0x51A7)
                if (positiveMod(glitchHash, tuning.glitchGlyphChanceDivisor) == 0) glitchHash else baseHash
            }
        }
        if (!keepGlyphAtDepth(baseHash, material, depth)) return ' '.code
        return material.glyphRamp[positiveMod(index, material.glyphRamp.length)].code
    }

    fun foreground(
        material: SurfaceMaterial,
        side: RayHitSide?,
        depth: Double,
        worldX: Double,
        worldZ: Double,
        vertical: Double,
        timeSeconds: Double,
    ): Int {
        var brightness = if (side == RayHitSide.X_FACE) tuning.sideFaceBrightness else 1.0
        val phase = (worldX + worldZ * 1.37) * material.phaseScale + timeSeconds * material.animationRate
        if (material.animation == SurfaceAnimation.SIGNAL_PULSE) {
            brightness *= tuning.signalPulseMinimumBrightness +
                (sin(phase) * 0.5 + 0.5) * (1.0 - tuning.signalPulseMinimumBrightness)
        } else if (material.animation == SurfaceAnimation.GLITCH_FLICKER) {
            val tick = floor(timeSeconds * minOf(material.animationRate, tuning.maximumGlitchRate)).toInt()
            if (
                positiveMod(
                    stableHash(floor(worldX).toInt(), floor(worldZ).toInt(), tick, material.id.hashCode()),
                    tuning.glitchFlashChanceDivisor,
                ) == 0
            ) {
                brightness *= tuning.glitchBrightnessBoost
            }
        }
        val shaded = multiplyRgb(samplePaletteColor(material, worldX, worldZ, vertical), brightness)
        val fogged = blend(shaded, palette[PaletteKeys.FOG], fogAmount(depth))
        return ensureNearContrast(fogged, background(material, side, depth), depth)
    }

    fun background(material: SurfaceMaterial, side: RayHitSide?, depth: Double = Double.POSITIVE_INFINITY): Int {
        var factor = when {
            ROAD_ASPHALT_TAG in material.tags -> ROAD_ASPHALT_BACKGROUND_BRIGHTNESS
            "solid-interior-wall" in material.tags && side == RayHitSide.X_FACE ->
                SOLID_INTERIOR_SIDE_BACKGROUND_BRIGHTNESS
            "solid-interior-wall" in material.tags -> SOLID_INTERIOR_FRONT_BACKGROUND_BRIGHTNESS
            "white-tile" in material.tags && "ceiling" in material.tags -> CEILING_TILE_BACKGROUND_BRIGHTNESS
            "white-tile" in material.tags && side == RayHitSide.X_FACE -> SIDE_TILE_BACKGROUND_BRIGHTNESS
            "white-tile" in material.tags -> FRONT_TILE_BACKGROUND_BRIGHTNESS
            side == RayHitSide.X_FACE -> SIDE_BACKGROUND_BRIGHTNESS
            else -> FRONT_BACKGROUND_BRIGHTNESS
        }
        if ("facade" in material.tags && "interior" !in material.tags && depth.isFinite()) {
            val nearSolidness = ((FACADE_FILL_DISTANCE - depth) / FACADE_FILL_DISTANCE).coerceIn(0.0, 1.0)
            val boost = if (side == RayHitSide.X_FACE) {
                CLOSE_FACADE_SIDE_BACKGROUND_BOOST
            } else {
                CLOSE_FACADE_FRONT_BACKGROUND_BOOST
            }
            factor += nearSolidness * boost
        }
        return multiplyRgb(palette[material.paletteKey], factor)
    }

    fun foreground(paletteKey: String, side: RayHitSide?, depth: Double): Int {
        val brightness = if (side == RayHitSide.X_FACE) tuning.sideFaceBrightness else 1.0
        val shaded = multiplyRgb(palette[paletteKey], brightness)
        val fogged = blend(shaded, palette[PaletteKeys.FOG], fogAmount(depth))
        return ensureNearContrast(fogged, background(paletteKey, side), depth)
    }

    fun background(paletteKey: String, side: RayHitSide?): Int {
        val factor = if (side == RayHitSide.X_FACE) SIDE_BACKGROUND_BRIGHTNESS else FRONT_BACKGROUND_BRIGHTNESS
        return multiplyRgb(palette[paletteKey], factor)
    }

    private fun samplePaletteColor(
        material: SurfaceMaterial,
        worldX: Double,
        worldZ: Double,
        vertical: Double,
    ): Int {
        if (material.accentPaletteKeys.isEmpty()) return palette[material.paletteKey]
        val hash = stableHash(
            floor(worldX * 3.0).toInt(),
            floor(worldZ * 3.0).toInt(),
            floor(vertical * 2.0).toInt(),
            material.id.hashCode(),
        )
        val accentSlots = when {
            "neon" in material.tags -> 4
            "interior" in material.tags -> 1
            else -> 2
        }
        if ((hash ushr 8 and 7) >= accentSlots) return palette[material.paletteKey]
        val accentIndex = positiveMod(hash ushr 16, material.accentPaletteKeys.size)
        return palette[material.accentPaletteKeys[accentIndex]]
    }

    private fun multiplyRgb(rgba: Int, factor: Double): Int {
        val red = (((rgba ushr 24) and 0xFF) * factor).toInt().coerceIn(0, 255)
        val green = (((rgba ushr 16) and 0xFF) * factor).toInt().coerceIn(0, 255)
        val blue = (((rgba ushr 8) and 0xFF) * factor).toInt().coerceIn(0, 255)
        return red shl 24 or (green shl 16) or (blue shl 8) or (rgba and 0xFF)
    }

    internal fun luminanceContrast(first: Int, second: Int): Double =
        kotlin.math.abs(luminance(first) - luminance(second))

    private fun fogAmount(depth: Double): Double = (
        (depth - tuning.fogStartDistance) /
            (tuning.fogEndDistance - tuning.fogStartDistance)
        ).coerceIn(0.0, 1.0)

    private fun keepGlyphAtDepth(baseHash: Int, material: SurfaceMaterial, depth: Double): Boolean {
        val density = 1.0 - fogAmount(depth) * (1.0 - tuning.fogEndGlyphDensity)
        val densityHash = stableHash(baseHash, material.id.hashCode(), DENSITY_HASH_SALT, 0)
        val bucket = positiveMod(densityHash, GLYPH_DENSITY_BUCKETS)
        return bucket < density * GLYPH_DENSITY_BUCKETS
    }

    private fun structuralGlyph(
        material: SurfaceMaterial,
        worldX: Double,
        worldZ: Double,
        vertical: Double,
        depth: Double,
    ): Int? = when {
        "wall-tile-grid" in material.tags -> gridGlyph(worldX + worldZ, vertical, WALL_TILE_SCALE)
        "ceiling-tile-grid" in material.tags -> gridGlyph(worldX, worldZ, CEILING_TILE_SCALE)
        depth <= FACADE_DETAIL_DISTANCE && "facade-window-grid" in material.tags ->
            detailGridGlyph(worldX + worldZ, vertical, FACADE_WINDOW_HORIZONTAL_SCALE, FACADE_WINDOW_VERTICAL_SCALE)
        depth <= FACADE_DETAIL_DISTANCE && "facade-ribbed" in material.tags ->
            ribbedFacadeGlyph(worldX + worldZ, vertical)
        depth <= ROAD_DETAIL_DISTANCE && "road-detail" in material.tags ->
            detailGridGlyph(worldX, worldZ, ROAD_SEAM_SCALE)
        depth <= SIDEWALK_DETAIL_DISTANCE && "sidewalk-detail" in material.tags ->
            detailGridGlyph(worldX, worldZ, SIDEWALK_SLAB_SCALE)
        else -> null
    }

    private fun ribbedFacadeGlyph(horizontal: Double, vertical: Double): Int? {
        val horizontalPhase = positiveFraction(horizontal * FACADE_RIB_HORIZONTAL_SCALE)
        val verticalPhase = positiveFraction(vertical * FACADE_RIB_VERTICAL_SCALE)
        val rib = horizontalPhase < FACADE_RIB_HALF_WIDTH || horizontalPhase > 1.0 - FACADE_RIB_HALF_WIDTH
        val belt = verticalPhase < FACADE_BELT_HALF_WIDTH || verticalPhase > 1.0 - FACADE_BELT_HALF_WIDTH
        return when {
            rib && belt -> '+'.code
            rib -> '|'.code
            belt -> '-'.code
            else -> null
        }
    }

    private fun gridGlyph(
        first: Double,
        second: Double,
        firstScale: Double,
        secondScale: Double = firstScale,
    ): Int {
        val firstPhase = positiveFraction(first * firstScale)
        val secondPhase = positiveFraction(second * secondScale)
        val firstGrout = firstPhase < TILE_GROUT_HALF_WIDTH || firstPhase > 1.0 - TILE_GROUT_HALF_WIDTH
        val secondGrout = secondPhase < TILE_GROUT_HALF_WIDTH || secondPhase > 1.0 - TILE_GROUT_HALF_WIDTH
        return when {
            firstGrout && secondGrout -> '+'.code
            firstGrout -> '|'.code
            secondGrout -> '-'.code
            else -> ' '.code
        }
    }

    private fun detailGridGlyph(
        first: Double,
        second: Double,
        firstScale: Double,
        secondScale: Double = firstScale,
    ): Int? = gridGlyph(first, second, firstScale, secondScale).takeUnless { it == ' '.code }

    private fun positiveFraction(value: Double): Double = value - floor(value)

    private fun ensureNearContrast(foreground: Int, background: Int, depth: Double): Int {
        if (depth > tuning.nearContrastDistance) return foreground
        val backgroundLuminance = luminance(background)
        val foregroundLuminance = luminance(foreground)
        if (kotlin.math.abs(foregroundLuminance - backgroundLuminance) >= tuning.minimumNearLuminanceContrast) {
            return foreground
        }
        val targetLuminance = if (foregroundLuminance >= backgroundLuminance) {
            (backgroundLuminance + tuning.minimumNearLuminanceContrast + CONTRAST_ROUNDING_MARGIN).coerceAtMost(1.0)
        } else {
            (backgroundLuminance - tuning.minimumNearLuminanceContrast - CONTRAST_ROUNDING_MARGIN).coerceAtLeast(0.0)
        }
        if (foregroundLuminance <= 0.0) {
            return blend(foreground, 0xFFFFFFFF.toInt(), targetLuminance)
        }
        return multiplyRgb(foreground, targetLuminance / foregroundLuminance)
    }

    private fun luminance(rgba: Int): Double {
        val red = (rgba ushr 24) and 0xFF
        val green = (rgba ushr 16) and 0xFF
        val blue = (rgba ushr 8) and 0xFF
        return (red * 0.2126 + green * 0.7152 + blue * 0.0722) / 255.0
    }

    private fun blend(from: Int, to: Int, amount: Double): Int {
        val inverse = 1.0 - amount
        val red = (((from ushr 24) and 0xFF) * inverse + ((to ushr 24) and 0xFF) * amount).toInt()
        val green = (((from ushr 16) and 0xFF) * inverse + ((to ushr 16) and 0xFF) * amount).toInt()
        val blue = (((from ushr 8) and 0xFF) * inverse + ((to ushr 8) and 0xFF) * amount).toInt()
        val alpha = (((from) and 0xFF) * inverse + ((to) and 0xFF) * amount).toInt()
        return red shl 24 or (green shl 16) or (blue shl 8) or alpha
    }

    private fun stableHash(a: Int, b: Int, c: Int, d: Int): Int {
        var value = 0x1B873593
        value = (value xor a) * -0x3361d2af
        value = (value xor b) * -0x3361d2af
        value = (value xor c) * -0x3361d2af
        value = (value xor d) * -0x3361d2af
        return value xor (value ushr 16)
    }

    private fun positiveMod(value: Int, divisor: Int): Int {
        val result = value % divisor
        return if (result < 0) result + divisor else result
    }

    companion object {
        private const val SIDE_BACKGROUND_BRIGHTNESS = 0.08
        private const val FRONT_BACKGROUND_BRIGHTNESS = 0.12
        private const val ROAD_ASPHALT_BACKGROUND_BRIGHTNESS = 0.42
        private const val SIDE_TILE_BACKGROUND_BRIGHTNESS = 0.34
        private const val FRONT_TILE_BACKGROUND_BRIGHTNESS = 0.46
        private const val CEILING_TILE_BACKGROUND_BRIGHTNESS = 0.38
        private const val SOLID_INTERIOR_SIDE_BACKGROUND_BRIGHTNESS = 0.28
        private const val SOLID_INTERIOR_FRONT_BACKGROUND_BRIGHTNESS = 0.36
        private const val FACADE_FILL_DISTANCE = 28.0
        private const val CLOSE_FACADE_SIDE_BACKGROUND_BOOST = 0.19
        private const val CLOSE_FACADE_FRONT_BACKGROUND_BOOST = 0.28
        private const val WALL_TILE_SCALE = 2.0
        private const val CEILING_TILE_SCALE = 0.5
        private const val TILE_GROUT_HALF_WIDTH = 0.10
        private const val FACADE_DETAIL_DISTANCE = 22.0
        private const val FACADE_WINDOW_HORIZONTAL_SCALE = 0.72
        private const val FACADE_WINDOW_VERTICAL_SCALE = 0.54
        private const val FACADE_RIB_HORIZONTAL_SCALE = 1.35
        private const val FACADE_RIB_VERTICAL_SCALE = 0.32
        private const val FACADE_RIB_HALF_WIDTH = 0.075
        private const val FACADE_BELT_HALF_WIDTH = 0.055
        private const val ROAD_DETAIL_DISTANCE = 18.0
        private const val SIDEWALK_DETAIL_DISTANCE = 16.0
        private const val ROAD_SEAM_SCALE = 0.25
        private const val SIDEWALK_SLAB_SCALE = 0.5
        private const val ROAD_ASPHALT_TAG = "road-asphalt"
        private const val CONTRAST_ROUNDING_MARGIN = 2.0 / 255.0
        private const val GLYPH_DENSITY_BUCKETS = 1024
        private const val DENSITY_HASH_SALT = 0x44454E53
    }
}
