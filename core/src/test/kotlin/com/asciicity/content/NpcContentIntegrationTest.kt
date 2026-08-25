package com.asciicity.content

import com.asciicity.entity.WorldEntityType
import com.asciicity.render.AsciiFrame
import com.asciicity.render.EntitySpriteRenderer
import com.asciicity.render.FontMetrics
import com.asciicity.render.ProjectionMetrics
import com.asciicity.render.ViewPose
import com.asciicity.system.CollisionSystem
import com.asciicity.world.EntityId
import java.nio.file.Path
import kotlin.math.atan2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NpcContentIntegrationTest {
    @Test
    fun `authored Rin resolves one semantic entity sprite dialogue and collision position`() {
        val content = ContentLoader().load(Path.of("."), "city-alpha")
        val npc = content.npcs.single()
        val entity = requireNotNull(content.world.entity(npc.entityId))

        assertEquals(EntityId("npc.rin"), npc.entityId)
        assertEquals("sprite.rin", npc.spriteId)
        assertEquals(270.0, npc.facingDegrees)
        assertEquals("Rin", npc.displayName)
        assertEquals("dialogue.rin.intro", npc.dialogueId)
        assertEquals(WorldEntityType.NPC, entity.type)
        assertEquals(45.5, entity.position.x)
        assertEquals(16.5, entity.position.z)
        assertEquals(0.24, entity.collisionRadius)
        assertTrue(entity.blocking)
        assertEquals(npc.displayName, entity.displayText)
        assertEquals(npc.dialogueId, entity.interactionId)
        assertFalse(content.world.grid.cellAt(entity.position.toGridCell()).solid)
        assertFalse(
            CollisionSystem(content.world).isPositionFree(
                entity.position,
                content.world.player.radius,
            ),
        )

        val player = content.world.player
        val deltaX = entity.position.x - player.position.x
        val deltaZ = entity.position.z - player.position.z
        val pose = ViewPose(
            position = player.position,
            eyeHeight = player.eyeHeight,
            yawRadians = ViewPose.normalizeYaw(atan2(deltaX, deltaZ)),
            pitchRadians = 0.0,
            horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
        )
        val frame = AsciiFrame(FRAME_COLUMNS, FRAME_ROWS)
        val metrics = ProjectionMetrics(
            columns = FRAME_COLUMNS,
            rows = FRAME_ROWS,
            pixelWidth = FRAME_COLUMNS * FontMetrics.CELL_WIDTH,
            pixelHeight = FRAME_ROWS * FontMetrics.CELL_HEIGHT,
            horizontalFovRadians = pose.horizontalFovRadians,
            pitchRadians = pose.pitchRadians,
        )
        val written = EntitySpriteRenderer().render(
            frame = frame,
            pose = pose,
            metrics = metrics,
            sprite = content.sprites.sprite(npc.spriteId),
            position = entity.position,
        )

        assertTrue(written > 0, "Rin's authored entity position must project through the shared sprite renderer")
        assertEquals("Rin", content.dialogues.dialogue(npc.dialogueId).speaker)
    }

    companion object {
        private const val FRAME_COLUMNS = 128
        private const val FRAME_ROWS = 40
    }
}
