package com.asciicity.render

import com.asciicity.TestContent
import com.asciicity.content.CellRegistry
import com.asciicity.content.ContentLoader
import com.asciicity.entity.WorldEntity
import com.asciicity.entity.WorldEntityType
import com.asciicity.world.CellDefinition
import com.asciicity.world.EntityId
import com.asciicity.world.WorldGrid
import com.asciicity.world.WorldPosition
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ParkFireflyRendererTest {
    @Test
    fun `park and nature maps derive capped lamp swarms including their waterfall areas`() {
        val city = TestContent.loaded
        val cityRenderer = ParkFireflyRenderer(
            city.world.allEntities(),
            city.world.grid,
            MaterialSampler(city.palette, city.presentation),
        )
        val reserve = ContentLoader().load(Path.of("."), "nature-reserve")
        val reserveRenderer = ParkFireflyRenderer(
            reserve.world.allEntities(),
            reserve.world.grid,
            MaterialSampler(reserve.palette, reserve.presentation),
        )
        val waterPark = ContentLoader().load(Path.of("."), "water-park")
        val waterParkRenderer = ParkFireflyRenderer(
            waterPark.world.allEntities(),
            waterPark.world.grid,
            MaterialSampler(waterPark.palette, waterPark.presentation),
        )

        assertEquals(4, cityRenderer.swarmCount)
        assertTrue("lamp.nature-cascade" in cityRenderer.swarmIds())
        assertTrue(cityRenderer.swarmIds().any { it.startsWith("lamp.park-") })
        assertEquals(4, reserveRenderer.swarmCount)
        assertTrue("lamp.reserve-cascade" in reserveRenderer.swarmIds())
        assertEquals(0, waterParkRenderer.swarmCount)
    }

    @Test
    fun `fireflies are deterministic animate and yield to nearer geometry`() {
        val palette = TestContent.loaded.palette
        val renderer = ParkFireflyRenderer(
            listOf(
                WorldEntity(
                    id = EntityId("lamp.firefly-test"),
                    type = WorldEntityType.LAMP,
                    position = WorldPosition(2.5, 6.5),
                    blocking = false,
                    interactionId = null,
                    displayText = null,
                    collisionRadius = 0.0,
                ),
            ),
            parkGrid(),
            MaterialSampler(palette),
        )
        val pose = ViewPose(
            position = WorldPosition(2.5, 1.5),
            eyeHeight = 0.6,
            yawRadians = 0.0,
            pitchRadians = 0.0,
            horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
        )
        val metrics = ProjectionMetrics(96, 30, 960, 540, pose.horizontalFovRadians, pose.pitchRadians)
        val first = emptyFrame(palette)
        val repeated = emptyFrame(palette)
        val advanced = emptyFrame(palette)

        val firstWrites = renderer.render(first, pose, metrics, 2.0)
        val repeatedWrites = renderer.render(repeated, pose, metrics, 2.0)
        renderer.render(advanced, pose, metrics, 3.5)

        assertTrue(firstWrites > 0)
        assertEquals(firstWrites, repeatedWrites)
        assertEquals(frameSnapshot(first), frameSnapshot(repeated))
        assertNotEquals(frameSnapshot(first), frameSnapshot(advanced))
        assertTrue(frameContains(first, '*'.code) || frameContains(first, 'o'.code))
        assertTrue(frameContainsEmissive(first))

        val occluded = emptyFrame(palette).apply { clear(depth = 1.0f) }
        assertEquals(0, renderer.render(occluded, pose, metrics, 2.0))
    }

    private fun parkGrid(): WorldGrid {
        val definition = CellDefinition(
            id = "test.park",
            token = 'p',
            groundMaterialId = "ground.park",
            facadeMaterialId = "facade.none",
            solid = false,
            opaque = false,
            height = 0,
            tags = setOf("park", "grass", "walkable"),
            interactionType = null,
        )
        return WorldGrid.fromResolvedIndices(
            width = 5,
            depth = 10,
            registry = CellRegistry.create(listOf(definition), TestContent.loaded.materials, "firefly-test"),
            cellIndices = IntArray(5 * 10),
        )
    }

    private fun emptyFrame(palette: RenderPalette) = AsciiFrame(96, 30).apply {
        clear(' '.code, palette[PaletteKeys.FOG], palette[PaletteKeys.SKY])
    }

    private fun frameSnapshot(frame: AsciiFrame): List<Int> = buildList {
        for (row in 0 until frame.height) for (column in 0 until frame.width) {
            add(frame.glyphAt(column, row))
            add(frame.foregroundAt(column, row))
            add(frame.backgroundAt(column, row))
            add(frame.depthAt(column, row).toBits())
        }
    }

    private fun frameContains(frame: AsciiFrame, glyph: Int): Boolean =
        (0 until frame.height).any { row -> (0 until frame.width).any { column -> frame.glyphAt(column, row) == glyph } }

    private fun frameContainsEmissive(frame: AsciiFrame): Boolean =
        (0 until frame.height).any { row -> (0 until frame.width).any { column -> frame.emissiveAt(column, row) > 0f } }
}
