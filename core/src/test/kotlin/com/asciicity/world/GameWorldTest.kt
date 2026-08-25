package com.asciicity.world

import com.asciicity.TestContent
import com.asciicity.entity.PlayerState
import com.asciicity.entity.WorldEntity
import com.asciicity.entity.WorldEntityType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GameWorldTest {
    private fun newWorld(): GameWorld {
        val cells = TestContent.loaded.cells
        val grid = WorldGrid.fromResolvedIndices(4, 4, cells, IntArray(16) { cells.indexForId("road") })
        return GameWorld("test-map", 1, grid, PlayerState(WorldPosition(1.5, 1.5), 0.0), emptyList())
    }

    @Test
    fun `entity add move query and remove keep spatial index consistent`() {
        val world = newWorld()
        val entity = WorldEntity(EntityId("npc.test"), WorldEntityType.NPC, WorldPosition(1.5, 1.5), true, "dialogue.test", "Test")

        world.addEntity(entity)
        assertEquals(listOf(entity), world.entitiesAt(GridCell(1, 1)))
        assertEquals(entity, world.entity(entity.id))

        world.moveEntity(entity.id, WorldPosition(2.5, 1.5))
        assertEquals(emptyList(), world.entitiesAt(GridCell(1, 1)))
        assertEquals(entity.copy(position = WorldPosition(2.5, 1.5)), world.entitiesAt(GridCell(2, 1)).single())

        assertEquals(entity.id, world.removeEntity(entity.id)?.id)
        assertNull(world.entity(entity.id))
        assertEquals(emptyList(), world.entitiesAt(GridCell(2, 1)))
    }

    @Test
    fun `duplicate IDs unknown moves bounds and time invariants fail`() {
        val world = newWorld()
        val entity = WorldEntity(EntityId("terminal.test"), WorldEntityType.TERMINAL, WorldPosition(1.5, 1.5), false, "terminal.test", null)
        world.addEntity(entity)

        assertFailsWith<IllegalStateException> { world.addEntity(entity) }
        assertFailsWith<IllegalArgumentException> { world.moveEntity(EntityId("npc.unknown"), WorldPosition(1.0, 1.0)) }
        assertFailsWith<IllegalArgumentException> { world.moveEntity(entity.id, WorldPosition(5.0, 1.0)) }
        assertFailsWith<IllegalArgumentException> { world.advanceTime(-0.1) }
        world.advanceTime(0.25)
        assertEquals(0.25, world.simulationTimeSeconds)
    }
}
