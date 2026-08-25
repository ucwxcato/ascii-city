package com.asciicity.render

import com.asciicity.TestContent
import com.asciicity.world.SurfaceAnimation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class MaterialSamplerTest {
    @Test
    fun `living data samples repeat at equal time and advance without world mutation`() {
        val content = TestContent.loaded
        val sampler = MaterialSampler(content.palette, content.presentation)
        val material = content.materials.material("ground.road")
        val beforeCells = content.world.gridSnapshot()

        val first = sampler.glyph(material, 12.5, 7.5, 0.0, 1.0)
        val repeated = sampler.glyph(material, 12.5, 7.5, 0.0, 1.0)
        val advanced = sampler.glyph(material, 12.5, 7.5, 0.0, 1.5)

        assertEquals(first, repeated)
        assertNotEquals(first, advanced)
        assertEquals(beforeCells, content.world.gridSnapshot())
    }

    @Test
    fun `facade accents add several deterministic neon colors without replacing the base`() {
        val content = TestContent.loaded
        val sampler = MaterialSampler(content.palette, content.presentation)
        val material = content.materials.material("facade.low")
        val colors = buildSet {
            for (x in 0 until 16) for (vertical in 0 until 8) {
                add(sampler.foreground(material, null, 0.0, x / 3.0, 2.25, vertical / 2.0, 4.0))
            }
        }

        assertTrue(content.palette[material.paletteKey] in colors)
        assertTrue(content.palette["neonGreen"] in colors)
        assertTrue(content.palette["neonOrange"] in colors)
        assertTrue(content.palette["windowLit"] in colors)
        assertEquals(
            sampler.foreground(material, null, 0.0, 1.25, 2.25, 3.0, 4.0),
            sampler.foreground(material, null, 0.0, 1.25, 2.25, 3.0, 4.0),
        )
    }

    @Test
    fun `road color remains neutral while its data glyphs animate`() {
        val content = TestContent.loaded
        val sampler = MaterialSampler(content.palette, content.presentation)
        val road = content.materials.material("ground.road")

        assertTrue(road.accentPaletteKeys.isEmpty())
        assertEquals(
            content.palette[road.paletteKey],
            sampler.foreground(road, null, 0.0, 3.25, 8.75, 0.0, 1.0),
        )
        assertNotEquals(
            sampler.glyph(road, 3.25, 8.75, 0.0, 1.0),
            sampler.glyph(road, 3.25, 8.75, 0.0, 1.5),
        )
        assertTrue(
            sampler.luminanceContrast(sampler.background(road, null), content.palette[PaletteKeys.SKY]) >= 0.04,
            "Road asphalt must read separately from the sky/void",
        )
    }

    @Test
    fun `water park surfaces form stable pale tile grids without brightening ground`() {
        val content = TestContent.loaded
        val sampler = MaterialSampler(content.palette, content.presentation)
        val wall = content.materials.material("facade.water-park-wall")
        val ceiling = content.materials.material("ceiling.water-park")
        val wallGlyphs = buildSet {
            for (horizontalStep in 0..20) for (verticalStep in 0..20) {
                add(
                    sampler.glyph(
                        wall,
                        horizontalStep / 20.0,
                        0.0,
                        verticalStep / 20.0,
                        0.0,
                    ),
                )
            }
        }
        val ceilingGlyphs = buildSet {
            for (x in 0..40) for (z in 0..40) {
                add(sampler.glyph(ceiling, x / 10.0, z / 10.0, 7.0, 0.0))
            }
        }

        assertTrue(setOf(' '.code, '+'.code, '-'.code, '|'.code).all { it in wallGlyphs })
        assertTrue(setOf(' '.code, '+'.code, '-'.code, '|'.code).all { it in ceilingGlyphs })
        assertNotEquals(
            sampler.background(wall.paletteKey, side = null),
            sampler.background(wall, side = null),
        )
        val road = content.materials.material("ground.road")
        assertNotEquals(
            sampler.background(road.paletteKey, side = null),
            sampler.background(road, side = null),
            "Road asphalt owns a distinct neutral surface value",
        )
    }

    @Test
    fun `casino wall uses a filled gold background while ordinary facades stay dark`() {
        val content = TestContent.loaded
        val sampler = MaterialSampler(content.palette, content.presentation)
        val casinoWall = content.materials.material("facade.casino-wall")
        val ordinaryWall = content.materials.material("facade.low")

        assertEquals(0x45340CFF, sampler.background(casinoWall, side = null))
        assertEquals(0x35280AFF, sampler.background(casinoWall, side = RayHitSide.X_FACE))
        assertNotEquals(
            sampler.background(ordinaryWall.paletteKey, side = null),
            sampler.background(casinoWall, side = null),
        )
    }

    @Test
    fun `exterior facades retain restrained solid fill through the middle distance`() {
        val content = TestContent.loaded
        val sampler = MaterialSampler(content.palette, content.presentation)
        val facade = content.materials.material("facade.low")
        val road = content.materials.material("ground.road")

        val nearFacade = sampler.background(facade, side = null, depth = 2.0)
        val middleFacade = sampler.background(facade, side = null, depth = 16.0)
        val outerMiddleFacade = sampler.background(facade, side = null, depth = 24.0)
        val farFacade = sampler.background(facade, side = null, depth = 40.0)
        val baseFacade = content.palette[facade.paletteKey]
        val nearRed = nearFacade ushr 24 and 0xFF
        val middleRed = middleFacade ushr 24 and 0xFF
        val outerMiddleRed = outerMiddleFacade ushr 24 and 0xFF
        val farRed = farFacade ushr 24 and 0xFF
        val baseRed = baseFacade ushr 24 and 0xFF

        assertTrue(nearRed > middleRed)
        assertTrue(middleRed > outerMiddleRed)
        assertTrue(outerMiddleRed > farRed, "Façade mass should remain visible at 24 world units")
        assertTrue(nearRed < baseRed / 2, "Near fill must preserve the ASCII foreground contrast")
        assertTrue(nearRed > baseRed / 3, "Near fill must make the building mass visibly solid")
        assertEquals(
            sampler.background(road, side = null, depth = 2.0),
            sampler.background(road, side = null, depth = 40.0),
        )
        val waterParkWall = content.materials.material("facade.water-park-wall")
        assertEquals(
            sampler.background(waterParkWall, side = null, depth = 2.0),
            sampler.background(waterParkWall, side = null, depth = 40.0),
            "Interior wall treatments own their existing fill independently",
        )
    }

    @Test
    fun `close facades and neutral ground gain structured detail that fades before the skyline`() {
        val content = TestContent.loaded
        val sampler = MaterialSampler(content.palette, content.presentation)
        val structuredFacades = listOf("facade.low", "facade.residential", "facade.tall")
        for (materialId in structuredFacades) {
            val material = content.materials.material(materialId)
            val closeGlyphs = buildSet {
                for (horizontalStep in 0..80) for (verticalStep in 0..80) {
                    add(
                        sampler.glyph(
                            material,
                            worldX = horizontalStep / 16.0,
                            worldZ = 2.0,
                            vertical = verticalStep / 16.0,
                            timeSeconds = 2.0,
                            depth = 8.0,
                        ),
                    )
                }
            }
            assertTrue('+'.code in closeGlyphs, "$materialId must show structural detail nearby")
        }

        val road = content.materials.material("ground.road")
        val sidewalk = content.materials.material("ground.sidewalk")
        assertTrue(road.accentPaletteKeys.isEmpty())
        assertTrue(sidewalk.accentPaletteKeys.isEmpty())
        for (material in listOf(road, sidewalk)) {
            val closeGlyphs = buildSet {
                for (x in 0..80) for (z in 0..80) {
                    add(sampler.glyph(material, x / 10.0, z / 10.0, 0.0, 2.0, depth = 6.0))
                }
            }
            assertTrue('+'.code in closeGlyphs, "${material.id} must show neutral surface structure nearby")
            assertTrue(
                sampler.glyph(material, 4.01, 8.01, 0.0, 2.0, depth = 30.0) != '+'.code,
                "${material.id} must not keep structural detail in the far ground",
            )
        }
    }

    private fun com.asciicity.world.GameWorld.gridSnapshot(): List<String> = buildList {
        grid.forEachCell { _, definition -> add(definition.id) }
    }
}
