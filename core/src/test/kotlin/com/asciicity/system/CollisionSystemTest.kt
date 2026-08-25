package com.asciicity.system

import com.asciicity.TestContent
import com.asciicity.entity.WorldEntity
import com.asciicity.entity.WorldEntityType
import com.asciicity.world.EntityId
import com.asciicity.world.GridCell
import com.asciicity.world.WorldPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CollisionSystemTest {
    @Test
    fun `semantic walls corners open cells and boundaries use circle geometry`() {
        val world = SystemTestWorld.create(solidCells = setOf(GridCell(4, 4), GridCell(4, 5)))
        val collision = CollisionSystem(world)
        assertTrue(collision.isPositionFree(WorldPosition(3.70, 4.5), 0.22))
        assertFalse(collision.isPositionFree(WorldPosition(3.80, 4.5), 0.22))
        assertFalse(collision.isPositionFree(WorldPosition(3.85, 3.85), 0.22))
        assertTrue(collision.isPositionFree(WorldPosition(2.5, 2.5), 0.22))
        assertFalse(collision.isPositionFree(WorldPosition(0.20, 2.5), 0.22))
        assertTrue(collision.isPositionFree(WorldPosition(0.22, 2.5), 0.22))

        assertEquals(
            world.grid.cellAt(2, 2).facadeMaterialId,
            world.grid.cellAt(4, 4).facadeMaterialId,
            "visual material must not decide collision",
        )

        val alleyWalls = buildSet {
            for (z in 1..8) {
                add(GridCell(3, z))
                add(GridCell(5, z))
            }
        }
        val alleyCollision = CollisionSystem(SystemTestWorld.create(solidCells = alleyWalls))
        assertTrue(alleyCollision.isPositionFree(WorldPosition(4.5, 4.5), 0.22))
        assertFalse(alleyCollision.isPositionFree(WorldPosition(4.5, 4.5), 0.51))
    }

    @Test
    fun `substeps prevent tunneling and axis separation slides along walls`() {
        val wall = (1..8).mapTo(mutableSetOf()) { GridCell(4, it) }
        val collision = CollisionSystem(SystemTestWorld.create(solidCells = wall))
        val stopped = collision.move(WorldPosition(2.5, 4.5), 6.0, 0.0, 0.22)
        assertTrue(stopped.x <= 3.78 + 1e-9)

        val slid = collision.move(WorldPosition(3.5, 2.5), 2.0, 3.0, 0.22)
        assertTrue(slid.x <= 3.78 + 1e-9)
        assertTrue(slid.z > 4.5)
        assertTrue(collision.isPositionFree(slid, 0.22))
    }

    @Test
    fun `blocking entities move with the spatial index while nonblockers and ignored self do not collide`() {
        val npc = entity("npc.blocker", WorldEntityType.NPC, 4.5, 4.5, blocking = true, radius = 0.30)
        val lamp = entity("lamp.passable", WorldEntityType.LAMP, 3.5, 4.5, blocking = false, radius = 0.8)
        val door = entity("door.closed", WorldEntityType.DOOR, 5.5, 5.5, blocking = true, radius = 0.45)
        val world = SystemTestWorld.create(entities = listOf(npc, lamp, door))
        val collision = CollisionSystem(world)
        assertTrue(collision.isPositionFree(lamp.position, 0.22))
        assertFalse(collision.isPositionFree(npc.position, 0.22))
        assertTrue(collision.isPositionFree(npc.position, 0.22, npc.id))
        assertFalse(collision.isPositionFree(door.position, 0.22))

        world.moveEntity(npc.id, WorldPosition(6.5, 4.5))
        assertTrue(collision.isPositionFree(WorldPosition(4.5, 4.5), 0.22))
        assertFalse(collision.isPositionFree(WorldPosition(6.5, 4.5), 0.22))
    }

    @Test
    fun `authored spawn has clearance from static and dynamic blockers`() {
        val content = TestContent.loaded
        assertTrue(
            CollisionSystem(content.world).isPositionFree(
                content.world.player.position,
                content.world.player.radius,
            ),
        )
    }

    private fun entity(
        id: String,
        type: WorldEntityType,
        x: Double,
        z: Double,
        blocking: Boolean,
        radius: Double,
    ) = WorldEntity(
        id = EntityId(id),
        type = type,
        position = WorldPosition(x, z),
        blocking = blocking,
        collisionRadius = radius,
        interactionId = null,
        displayText = null,
    )
}
