package com.asciicity.render

import com.asciicity.TestContent
import com.asciicity.world.SurfaceAnimation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PresentationTuningTest {
    @Test
    fun `neutral surface roles keep distinct authored values`() {
        val content = TestContent.loaded

        assertNotEquals(content.palette[PaletteKeys.FOG], content.palette[PaletteKeys.WALL_FRONT])
        assertNotEquals(content.palette[PaletteKeys.SIDEWALK], content.palette[PaletteKeys.WALL_SIDE])
        assertTrue(content.presentation.sideFaceBrightness <= 0.70)
    }

    @Test
    fun `fog approaches the configured color smoothly and reaches it at fog end`() {
        val content = TestContent.loaded
        val tuning = content.presentation
        val sampler = MaterialSampler(content.palette, tuning)
        val material = content.materials.material("facade.park-metal")
        val fog = content.palette[PaletteKeys.FOG]
        val depths = listOf(
            0.0,
            tuning.fogStartDistance,
            20.0,
            30.0,
            40.0,
            tuning.fogEndDistance,
        )
        val colors = depths.map { depth ->
            sampler.foreground(material, null, depth, 2.5, 3.5, 1.0, 0.0)
        }
        val distanceFromFog = colors.map { sampler.luminanceContrast(it, fog) }

        assertEquals(colors[0], colors[1], "fog must not alter near surfaces before fogStartDistance")
        assertTrue(distanceFromFog.zipWithNext().all { (near, far) -> far <= near + 1e-9 })
        assertEquals(fog, colors.last())
    }

    @Test
    fun `distance fog thins glyphs monotonically while preserving a readable skyline`() {
        val content = TestContent.loaded
        val tuning = content.presentation
        val sampler = MaterialSampler(content.palette, tuning)
        val material = content.materials.material("facade.tall")
        val sampleCount = 64 * 64
        val depths = listOf(
            tuning.fogStartDistance,
            (tuning.fogStartDistance + tuning.fogEndDistance) * 0.5,
            tuning.fogEndDistance,
            tuning.fogEndDistance + 20.0,
        )
        val visibleCounts = depths.map { depth ->
            var visible = 0
            for (x in 0 until 64) for (z in 0 until 64) {
                val glyph = sampler.glyph(
                    material,
                    worldX = x / 7.0,
                    worldZ = z / 7.0,
                    vertical = 1.0,
                    timeSeconds = 0.0,
                    depth = depth,
                )
                if (glyph != ' '.code) visible += 1
            }
            visible
        }

        assertEquals(sampleCount, visibleCounts.first(), "fog must not thin glyphs at its start distance")
        assertTrue(visibleCounts.zipWithNext().all { (near, far) -> far <= near })
        assertEquals(visibleCounts[2], visibleCounts[3], "glyph density must clamp beyond fog end")
        val endDensity = visibleCounts[2].toDouble() / sampleCount
        assertTrue(
            kotlin.math.abs(endDensity - tuning.fogEndGlyphDensity) <= 0.03,
            "expected end density ${tuning.fogEndGlyphDensity}, sampled $endDensity",
        )
    }

    @Test
    fun `near road and obstacle samples retain configured luminance contrast`() {
        val content = TestContent.loaded
        val tuning = content.presentation
        val sampler = MaterialSampler(content.palette, tuning)
        val materialIds = buildSet {
            add("ground.road")
            content.world.grid.forEachCell { _, definition ->
                if (definition.solid && definition.facadeMaterialId != "facade.none") add(definition.facadeMaterialId)
            }
        }

        for (materialId in materialIds) {
            val material = content.materials.material(materialId)
            for (side in listOf(null, RayHitSide.X_FACE)) {
                val foreground = sampler.foreground(material, side, 2.0, 3.25, 4.75, 1.0, 2.0)
                val background = sampler.background(material, side)
                assertTrue(
                    sampler.luminanceContrast(foreground, background) + 1e-9 >=
                        tuning.minimumNearLuminanceContrast,
                    "$materialId side=$side fell below near contrast floor",
                )
            }
        }
    }

    @Test
    fun `glitch materials obey the reduced flicker cap`() {
        val content = TestContent.loaded
        val glitchMaterials = content.materials.all().filter { it.animation == SurfaceAnimation.GLITCH_FLICKER }

        assertTrue(glitchMaterials.isNotEmpty())
        assertTrue(glitchMaterials.all { it.animationRate <= content.presentation.maximumGlitchRate })
        assertTrue(content.presentation.glitchGlyphChanceDivisor >= 17)
        assertTrue(content.presentation.glitchFlashChanceDivisor >= 29)
        assertTrue(content.presentation.glitchBrightnessBoost <= 1.15)
    }
}
