package com.asciicity.content

import com.asciicity.TestContent
import com.asciicity.world.CellDefinition
import com.asciicity.world.SurfaceAnimation
import com.asciicity.world.SurfaceMaterial
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RegistryValidationTest {
    private val palette = TestContent.loaded.palette
    private val materials = TestContent.loaded.materials

    @Test
    fun `material registry rejects duplicate IDs unknown palette and invalid animation values`() {
        val valid = SurfaceMaterial("test.valid", ".#", "fog", SurfaceAnimation.DATA_STREAM, 2.0, 0.2, emptySet())
        val duplicate = assertFailsWith<ContentValidationException> {
            MaterialRegistry.create(listOf(valid, valid.copy()), palette, "fixture-materials.json")
        }
        assertTrue(duplicate.message.orEmpty().contains("duplicate material ID"))

        val unknownPalette = assertFailsWith<ContentValidationException> {
            MaterialRegistry.create(listOf(valid.copy(paletteKey = "missing-color")), palette, "fixture-materials.json")
        }
        assertTrue(unknownPalette.message.orEmpty().contains("unknown palette key 'missing-color'"))

        val unknownAccent = assertFailsWith<ContentValidationException> {
            MaterialRegistry.create(
                listOf(valid.copy(accentPaletteKeys = listOf("missing-accent"))),
                palette,
                "fixture-materials.json",
            )
        }
        assertTrue(unknownAccent.message.orEmpty().contains("accentPaletteKeys[0]"))
        assertTrue(unknownAccent.message.orEmpty().contains("unknown palette key 'missing-accent'"))

        val invalidRate = assertFailsWith<ContentValidationException> {
            MaterialRegistry.create(listOf(valid.copy(animationRate = -1.0)), palette, "fixture-materials.json")
        }
        assertTrue(invalidRate.message.orEmpty().contains("animationRate"))
    }

    @Test
    fun `cell registry rejects duplicate IDs tokens negative height and unknown materials`() {
        val valid = CellDefinition("test.open", 'o', "ground.road", "facade.none", false, false, 0, emptySet(), null)

        val duplicateId = assertFailsWith<ContentValidationException> {
            CellRegistry.create(listOf(valid, valid.copy(token = 'p')), materials, "fixture-cells.json")
        }
        assertTrue(duplicateId.message.orEmpty().contains("duplicate cell ID"))

        val duplicateToken = assertFailsWith<ContentValidationException> {
            CellRegistry.create(listOf(valid, valid.copy(id = "test.other")), materials, "fixture-cells.json")
        }
        assertTrue(duplicateToken.message.orEmpty().contains("duplicate cell token"))

        val negativeHeight = assertFailsWith<ContentValidationException> {
            CellRegistry.create(listOf(valid.copy(height = -1)), materials, "fixture-cells.json")
        }
        assertTrue(negativeHeight.message.orEmpty().contains("height"))

        val unknownMaterial = assertFailsWith<ContentValidationException> {
            CellRegistry.create(listOf(valid.copy(groundMaterialId = "ground.missing")), materials, "fixture-cells.json")
        }
        assertTrue(unknownMaterial.message.orEmpty().contains("unknown material 'ground.missing'"))
    }
}
