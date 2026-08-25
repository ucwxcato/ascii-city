package com.asciicity.content

import com.asciicity.entity.WorldEntityType
import com.asciicity.world.EntityId
import com.asciicity.world.GridCell
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CityAlphaIntegrationTest {
    @Test
    fun `city alpha is deterministic reachable varied and complete`() {
        val first = ContentLoader().load(Path.of("."), "city-alpha")
        val second = ContentLoader().load(Path.of("."), "city-alpha")
        val report = ContentVerifier.verify(first)

        assertEquals(96, first.world.grid.width)
        assertEquals(96, first.world.grid.depth)
        assertEquals(GridCell(28, 13), first.world.player.position.toGridCell())
        assertFalse(first.world.grid.cellAt(first.world.player.position.toGridCell()).solid)
        assertEquals(9216, report.cellCounts.values.sum())
        assertEquals(setOf(0, 1, 2, 3, 4, 6, 7, 8, 9, 10, 12, 15, 16, 22), report.heightCounts.keys)
        assertEquals("building.garden-home-1", first.world.grid.cellAt(GridCell(0, 95)).id)
        assertEquals("building.garden-home-2", first.world.grid.cellAt(GridCell(27, 95)).id)
        assertEquals("park.fountain", first.world.grid.cellAt(GridCell(88, 90)).id)
        assertEquals("park.path", first.world.grid.cellAt(GridCell(28, 13)).id)
        assertEquals("park.tree", first.world.grid.cellAt(GridCell(21, 13)).id)
        assertEquals("park.pond", first.world.grid.cellAt(GridCell(24, 25)).id)
        assertEquals("park.fountain", first.world.grid.cellAt(GridCell(26, 24)).id)
        assertEquals("park.bench", first.world.grid.cellAt(GridCell(26, 18)).id)
        assertEquals("park.bin", first.world.grid.cellAt(GridCell(29, 19)).id)
        assertEquals("park.pavilion", first.world.grid.cellAt(GridCell(37, 24)).id)
        assertEquals("nature.pine", first.world.grid.cellAt(GridCell(0, 27)).id)
        assertEquals("nature.rock", first.world.grid.cellAt(GridCell(1, 26)).id)
        assertEquals("nature.waterfall", first.world.grid.cellAt(GridCell(4, 26)).id)
        assertEquals("nature.trail", first.world.grid.cellAt(GridCell(2, 25)).id)
        assertEquals("nature.tent", first.world.grid.cellAt(GridCell(8, 23)).id)
        assertEquals("nature.bridge", first.world.grid.cellAt(GridCell(4, 21)).id)
        assertEquals("park.pond", first.world.grid.cellAt(GridCell(4, 25)).id)
        assertFalse(first.world.grid.cellAt(GridCell(2, 25)).solid)
        assertFalse(first.world.grid.cellAt(GridCell(4, 21)).solid)
        val solidNatureCells = listOf(
            GridCell(0, 27),
            GridCell(1, 26),
            GridCell(4, 26),
            GridCell(8, 23),
            GridCell(4, 25),
        )
        for (solidCell in solidNatureCells) {
            assertTrue(first.world.grid.cellAt(solidCell).solid, "$solidCell must retain semantic collision")
        }
        for (cellId in setOf(
                "building.garden-home-1",
                "building.garden-home-2",
                "building.residential-1",
                "building.residential-2",
                "building.residential-3",
                "building.residential-4",
                "building.residential-5",
                "nature.trail",
                "nature.pine",
                "nature.tent",
                "nature.bridge",
                "nature.waterfall",
                "nature.rock",
            )
        ) {
            assertTrue(report.cellCounts.getValue(cellId) > 0, "$cellId must be authored in city-alpha")
        }
        var natureRiverCellCount = 0
        for (z in 13..27) {
            for (x in 0..11) {
                if (first.world.grid.cellAt(GridCell(x, z)).id == "park.pond") natureRiverCellCount += 1
            }
        }
        assertEquals(23, natureRiverCellCount)
        val nonInteriorMaterialIds = first.materials.all()
            .filterNot { "interior" in it.tags }
            .mapTo(sortedSetOf()) { it.id }
        assertEquals(nonInteriorMaterialIds, report.materialCellCounts.keys)
        assertEquals(
            listOf(
                "sprite.rin",
                "sprite.terminal",
                "sprite.door",
                "sprite.car.taxi",
                "sprite.car.sedan",
                "sprite.car.van",
                "sprite.pedestrian.coat",
                "sprite.pedestrian.umbrella",
                "sprite.pedestrian.courier",
            ),
            first.sprites.all().map { it.id },
        )
        assertEquals(listOf("npc.rin"), first.npcs.map { it.entityId.value })
        assertEquals(
            listOf(
                "car.night-taxi",
                "car.neon-sedan",
                "car.cargo-van",
                "car.arcade-coupe",
                "car.market-cab",
                "car.park-delivery",
            ),
            first.trafficCars.map { it.id },
        )
        for (car in first.trafficCars) {
            for (waypoint in car.waypoints) {
                assertTrue("road" in first.world.grid.cellAt(waypoint.toGridCell()).tags)
            }
        }
        assertTrue(report.reachableCellCount > 5_000)
        assertTrue(report.debugFootprint.lines().size >= 24)
        val types = first.world.allEntities().map { it.type }.toSet()
        assertTrue(WorldEntityType.NPC in types)
        assertTrue(WorldEntityType.AMBIENT_ACTOR in types)
        assertTrue(WorldEntityType.TERMINAL in types)
        assertTrue(WorldEntityType.LAMP in types)
        assertEquals(5, first.atmosphericEmitters.size)
        assertEquals(4, first.ambientEventEmitters.size)
        assertEquals(
            listOf("ambient.courier-market", "ambient.umbrella-archive", "ambient.walker-park"),
            first.ambientActors.map { it.entityId.value },
        )
        for (actor in first.ambientActors) {
            val entity = requireNotNull(first.world.entity(actor.entityId))
            assertEquals(WorldEntityType.AMBIENT_ACTOR, entity.type)
            assertEquals(actor.waypoints.first(), entity.position)
            assertTrue(entity.blocking)
            assertTrue(actor.waypoints.all { "walkable" in first.world.grid.cellAt(it.toGridCell()).tags })
        }
        val reachable = WorldReachability.reachableCells(first.world.grid, first.world.player.position.toGridCell())
        val cascadeRelay = first.world.entity(EntityId("terminal.relay-01"))
        requireNotNull(cascadeRelay)
        assertEquals(WorldEntityType.TERMINAL, cascadeRelay.type)
        assertEquals(GridCell(2, 25), cascadeRelay.position.toGridCell())
        assertEquals("CASCADE RELAY", cascadeRelay.displayText)
        assertTrue(cascadeRelay.position.toGridCell() in reachable)
        val casinoDoor = requireNotNull(first.world.entity(EntityId("door.last-light-casino")))
        assertEquals(WorldEntityType.DOOR, casinoDoor.type)
        assertEquals(GridCell(26, 4), casinoDoor.position.toGridCell())
        assertEquals("casino", casinoDoor.destinationMapId)
        assertEquals(1.5, casinoDoor.collisionRadius)
        assertFalse(casinoDoor.blocking)
        for (entity in first.world.allEntities().filter { it.type in setOf(WorldEntityType.NPC, WorldEntityType.TERMINAL) }) {
            assertTrue(entity.position.toGridCell() in reachable, "${entity.id} must be reachable")
        }

        for (z in 0 until 96) {
            for (x in 0 until 96) {
                assertEquals(
                    first.world.grid.cellAt(GridCell(x, z)).id,
                    second.world.grid.cellAt(GridCell(x, z)).id,
                )
            }
        }
    }
}
