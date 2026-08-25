package com.asciicity.system

import com.asciicity.entity.WorldEntity
import com.asciicity.entity.WorldEntityType
import com.asciicity.render.ViewPose
import com.asciicity.render.AsciiFontProfile
import com.asciicity.world.EntityId
import com.asciicity.world.GridCell
import com.asciicity.world.WorldPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InteractionSystemTest {
    @Test
    fun `centered terminal is selected and exposes stable data`() {
        val terminal = terminal("terminal.near", 2.5, 3.5)
        val system = InteractionSystem(SystemTestWorld.create(entities = listOf(terminal)))

        val target = system.findTarget(pose(), 128, 40)

        requireNotNull(target)
        assertEquals(terminal.id, target.entityId)
        assertEquals("terminal.test", target.interactionId)
        assertEquals("TEST NODE", target.label)
        assertEquals(1.0, target.depth, 1e-9)
    }

    @Test
    fun `dense center ray preserves centered interaction targeting`() {
        val terminal = terminal("terminal.dense", 2.5, 3.5)
        val system = InteractionSystem(SystemTestWorld.create(entities = listOf(terminal)))

        assertEquals(
            terminal.id,
            system.findTarget(pose(), 160, 48, AsciiFontProfile.DENSE)?.entityId,
        )
    }

    @Test
    fun `off-center behind and pitched-away targets are rejected`() {
        val offCenter = InteractionSystem(
            SystemTestWorld.create(entities = listOf(terminal("terminal.off", 3.2, 3.5))),
        )
        val behind = InteractionSystem(
            SystemTestWorld.create(entities = listOf(terminal("terminal.behind", 2.5, 1.5))),
        )
        val centered = InteractionSystem(
            SystemTestWorld.create(entities = listOf(terminal("terminal.center", 2.5, 3.5))),
        )

        assertNull(offCenter.findTarget(pose(), 128, 40))
        assertNull(behind.findTarget(pose(), 128, 40))
        assertNull(centered.findTarget(pose(pitchDegrees = 55.0), 128, 40))
        assertNull(centered.findTarget(pose(pitchDegrees = -55.0), 128, 40))
    }

    @Test
    fun `range boundary is inclusive and farther targets are rejected`() {
        val boundary = InteractionSystem(
            SystemTestWorld.create(entities = listOf(terminal("terminal.boundary", 2.5, 4.25))),
        )
        val beyond = InteractionSystem(
            SystemTestWorld.create(entities = listOf(terminal("terminal.beyond", 2.5, 4.251))),
        )

        assertEquals("terminal.boundary", boundary.findTarget(pose(), 128, 40)?.entityId?.value)
        assertNull(beyond.findTarget(pose(), 128, 40))
    }

    @Test
    fun `opaque wall rejects a target that would otherwise be centered`() {
        val world = SystemTestWorld.create(
            solidCells = setOf(GridCell(2, 3)),
            entities = listOf(terminal("terminal.hidden", 2.5, 4.0)),
        )

        assertNull(InteractionSystem(world).findTarget(pose(), 128, 40))
    }

    @Test
    fun `nearest centered entity wins overlapping depth`() {
        val world = SystemTestWorld.create(
            entities = listOf(
                terminal("terminal.far", 2.5, 3.8),
                terminal("terminal.near", 2.5, 3.2),
            ),
        )

        assertEquals("terminal.near", InteractionSystem(world).findTarget(pose(), 96, 30)?.entityId?.value)
    }

    @Test
    fun `terminal NPC and stale targets produce typed outcomes`() {
        val terminal = terminal("terminal.test-node", 2.5, 3.5)
        val npc = WorldEntity(
            id = EntityId("npc.test"),
            type = WorldEntityType.NPC,
            position = WorldPosition(2.5, 3.0),
            blocking = true,
            interactionId = "dialogue.test",
            displayText = "TEST NPC",
            collisionRadius = 0.24,
        )
        val world = SystemTestWorld.create(entities = listOf(terminal, npc))
        val system = InteractionSystem(world)

        assertEquals(
            InteractionOutcome.StartDialogue("terminal.test"),
            system.interact(targetFor(terminal)),
        )

        assertEquals(
            InteractionOutcome.StartDialogue("dialogue.test"),
            system.interact(targetFor(npc)),
        )
        val casinoDoor = WorldEntity(
            id = EntityId("door.last-light-casino"),
            type = WorldEntityType.DOOR,
            position = WorldPosition(2.5, 3.0),
            blocking = false,
            interactionId = "door.last-light-casino",
            displayText = "LAST LIGHT CASINO",
            destinationMapId = "casino",
        )
        world.addEntity(casinoDoor)
        assertEquals(
            InteractionOutcome.EnterMap("casino"),
            system.interact(targetFor(casinoDoor)),
        )
        val wideDoor = WorldEntity(
            id = EntityId("door.wide"),
            type = WorldEntityType.DOOR,
            position = WorldPosition(2.5, 3.5),
            blocking = false,
            interactionId = "door.wide",
            displayText = "WIDE DOOR",
            collisionRadius = 1.5,
            destinationMapId = "city-alpha",
            destinationPosition = WorldPosition(1.5, 1.5),
            destinationYawDegrees = 180.0,
        )
        world.addEntity(wideDoor)
        val wideTarget = requireNotNull(system.findTarget(pose().copy(position = WorldPosition(3.7, 2.5)), 128, 40))
        assertEquals(wideDoor.id, wideTarget.entityId)
        assertEquals(
            InteractionOutcome.EnterMap("city-alpha", WorldPosition(1.5, 1.5), 180.0),
            system.interact(wideTarget),
        )
        assertEquals(
            InteractionOutcome.Error("TARGET DATA IS INVALID"),
            system.interact(targetFor(npc).copy(interactionId = "dialogue.wrong")),
        )

        world.removeEntity(terminal.id)
        assertEquals(
            InteractionOutcome.Error("TARGET NO LONGER AVAILABLE"),
            system.interact(targetFor(terminal)),
        )
    }

    private fun terminal(id: String, x: Double, z: Double) = WorldEntity(
        id = EntityId(id),
        type = WorldEntityType.TERMINAL,
        position = WorldPosition(x, z),
        blocking = false,
        interactionId = "terminal.test",
        displayText = "TEST NODE",
        collisionRadius = 0.24,
    )

    private fun targetFor(entity: WorldEntity) = InteractionTarget(
        entityId = entity.id,
        interactionId = requireNotNull(entity.interactionId),
        label = requireNotNull(entity.displayText),
        depth = 1.0,
    )

    private fun pose(pitchDegrees: Double = 0.0) = ViewPose(
        position = WorldPosition(2.5, 2.5),
        eyeHeight = 0.60,
        yawRadians = 0.0,
        pitchRadians = Math.toRadians(pitchDegrees),
        horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
    )
}
