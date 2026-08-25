package com.asciicity.system

import com.asciicity.TestContent
import com.asciicity.entity.AmbientActor
import com.asciicity.entity.PlayerState
import com.asciicity.entity.WorldEntity
import com.asciicity.entity.WorldEntityType
import com.asciicity.world.EntityId
import com.asciicity.world.WorldPosition
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AmbientActorSystemTest {
    @Test
    fun `pedestrian follows its authored route and updates the authoritative entity`() {
        val start = WorldPosition(2.5, 4.5)
        val end = WorldPosition(2.5, 6.5)
        val id = EntityId("ambient.test-walker")
        val world = SystemTestWorld.create(
            playerPosition = WorldPosition(8.5, 8.5),
            entities = listOf(entity(id, start)),
        )
        val actor = AmbientActor(id, "sprite.pedestrian.coat", 0.5, 1.0, 0.0, listOf(start, end))
        val system = AmbientActorSystem(world, listOf(actor), TestContent.loaded.sprites)

        repeat(241) { system.step(1.0 / 60.0) }

        assertEquals(end, system.positionAt(0))
        assertEquals(end, requireNotNull(world.entity(id)).position)
        repeat(30) { system.step(1.0 / 60.0) }
        assertEquals(end, system.positionAt(0), "the authored endpoint dwell must hold the pedestrian")
    }

    @Test
    fun `pedestrian waits for the player without overlap and resumes when clear`() {
        val start = WorldPosition(2.5, 3.5)
        val end = WorldPosition(2.5, 7.5)
        val id = EntityId("ambient.test-blocked")
        val world = SystemTestWorld.create(
            playerPosition = WorldPosition(2.5, 5.5),
            entities = listOf(entity(id, start)),
        )
        val actor = AmbientActor(id, "sprite.pedestrian.coat", 0.6, 1.0, 0.0, listOf(start, end))
        val system = AmbientActorSystem(world, listOf(actor), TestContent.loaded.sprites)

        repeat(300) { system.step(1.0 / 60.0) }
        val blocked = system.positionAt(0)
        assertTrue(
            hypot(blocked.x - world.player.position.x, blocked.z - world.player.position.z) >=
                0.22 + world.player.radius,
        )
        world.updatePlayer(PlayerState(WorldPosition(8.5, 8.5), 0.0))
        repeat(250) { system.step(1.0 / 60.0) }

        assertEquals(end, system.positionAt(0))
        assertEquals(end, requireNotNull(world.entity(id)).position)
    }

    private fun entity(id: EntityId, position: WorldPosition) = WorldEntity(
        id = id,
        type = WorldEntityType.AMBIENT_ACTOR,
        position = position,
        blocking = true,
        interactionId = null,
        displayText = null,
        collisionRadius = 0.22,
    )
}
