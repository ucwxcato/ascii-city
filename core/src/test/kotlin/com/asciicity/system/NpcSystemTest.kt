package com.asciicity.system

import com.asciicity.TestContent
import com.asciicity.entity.Npc
import com.asciicity.entity.WorldEntity
import com.asciicity.entity.WorldEntityType
import com.asciicity.render.AsciiFrame
import com.asciicity.render.EntitySpriteRenderer
import com.asciicity.render.FontMetrics
import com.asciicity.render.ProjectionMetrics
import com.asciicity.render.ViewPose
import com.asciicity.render.WorldAsciiRenderer
import com.asciicity.world.EntityId
import com.asciicity.world.WorldPosition
import java.lang.management.ManagementFactory
import kotlin.math.atan2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NpcSystemTest {
    private val content = TestContent.loaded

    @Test
    fun `npc definitions render in stable entity id order`() {
        val alpha = npc("npc.alpha", "Alpha", "dialogue.alpha", WorldPosition(2.5, 4.5))
        val zeta = npc("npc.zeta", "Zeta", "dialogue.zeta", WorldPosition(3.5, 4.5))
        val world = SystemTestWorld.create(entities = listOf(zeta.second, alpha.second))

        val system = NpcSystem(world, listOf(zeta.first, alpha.first), content.sprites)

        assertEquals(2, system.npcCount)
        assertEquals(EntityId("npc.alpha"), system.entityIdAt(0))
        assertEquals(EntityId("npc.zeta"), system.entityIdAt(1))
    }

    @Test
    fun `five minute approach turn away resize and reapproach trace preserves Rin state and collision`() {
        val world = content.world
        val rin = content.npcs.single()
        val originalEntity = requireNotNull(world.entity(rin.entityId))
        val system = NpcSystem(world, content.npcs, content.sprites)
        val farPose = lookAt(world.player.position, originalEntity.position)
        val nearPose = lookAt(WorldPosition(43.5, 16.5), originalEntity.position)
        val awayPose = nearPose.copy(yawRadians = ViewPose.normalizeYaw(nearPose.yawRadians + Math.PI))
        val reapproachPose = lookAt(WorldPosition(45.5, 14.5), originalEntity.position)
        val largeFrame = AsciiFrame(128, 40)
        val smallFrame = AsciiFrame(96, 30)
        val largeMetrics = metrics(largeFrame, farPose)
        val smallMetrics = metrics(smallFrame, farPose)
        var visibleFrames = 0
        var culledFrames = 0

        repeat(FIVE_MINUTE_FRAME_COUNT) { frameIndex ->
            val phase = frameIndex / PHASE_FRAME_COUNT
            val frame = if (phase % 2 == 0) largeFrame else smallFrame
            val activeMetrics = if (frame === largeFrame) largeMetrics else smallMetrics
            val pose = when (phase) {
                0 -> farPose
                1 -> nearPose
                2 -> awayPose
                else -> reapproachPose
            }
            frame.clear()
            val writes = system.render(frame, pose, activeMetrics)
            if (writes > 0) visibleFrames += 1 else culledFrames += 1
        }

        assertEquals(PHASE_FRAME_COUNT * 3, visibleFrames)
        assertEquals(PHASE_FRAME_COUNT, culledFrames)
        assertEquals(originalEntity, world.entity(rin.entityId))
        assertFalse(CollisionSystem(world).isPositionFree(originalEntity.position, world.player.radius))
    }

    @Test
    fun `nearer frame depth occludes Rin without removing her world entity`() {
        val rin = content.npcs.single()
        val entity = requireNotNull(content.world.entity(rin.entityId))
        val system = NpcSystem(content.world, content.npcs, content.sprites)
        val pose = lookAt(WorldPosition(43.5, 16.5), entity.position)
        val frame = AsciiFrame(96, 30)
        val metrics = metrics(frame, pose)
        for (row in 0 until frame.height) {
            for (column in 0 until frame.width) {
                frame.set(column, row, '#'.code, 0xFFFFFFFF.toInt(), 0x000000FF, 1.0f)
            }
        }

        assertEquals(0, system.render(frame, pose, metrics))
        assertEquals(entity, content.world.entity(rin.entityId))
        assertFalse(CollisionSystem(content.world).isPositionFree(entity.position, content.world.player.radius))
    }

    @Test
    fun `world renderer composes Rin from authoritative position between geometry and later passes`() {
        val rin = content.npcs.single()
        val entity = requireNotNull(content.world.entity(rin.entityId))
        val system = NpcSystem(content.world, content.npcs, content.sprites)
        val renderer = WorldAsciiRenderer(
            world = content.world,
            materials = content.materials,
            palette = content.palette,
            presentation = content.presentation,
            facadeSigns = content.facadeSigns,
            npcSystem = system,
        )
        val frame = AsciiFrame(128, 40)
        val pose = lookAt(WorldPosition(43.5, 16.5), entity.position)

        renderer.render(frame, pose, 4.0)

        assertTrue(renderer.stats.entitySpriteWrites > 0)
        assertTrue((0 until frame.height).any { row ->
            (0 until frame.width).any { column -> frame.depthAt(column, row) == 2.0f }
        })
    }

    @Test
    fun `missing or mismatched npc entities fail instead of silently desynchronizing`() {
        val definition = npc("npc.test", "Test", "dialogue.test", WorldPosition(2.5, 4.5))
        val missingWorld = SystemTestWorld.create()
        assertFailsWith<IllegalArgumentException> {
            NpcSystem(missingWorld, listOf(definition.first), content.sprites)
        }

        val world = SystemTestWorld.create(entities = listOf(definition.second))
        val system = NpcSystem(world, listOf(definition.first), content.sprites)
        val movedPosition = WorldPosition(2.5, 5.5)
        world.moveEntity(definition.second.id, movedPosition)
        val movedFrame = AsciiFrame(96, 30)
        val movedPose = lookAt(WorldPosition(2.5, 2.5), movedPosition)
        assertTrue(system.render(movedFrame, movedPose, metrics(movedFrame, movedPose)) > 0)
        assertTrue((0 until movedFrame.height).any { row ->
            (0 until movedFrame.width).any { column -> movedFrame.depthAt(column, row) == 3.0f }
        })

        world.removeEntity(definition.second.id)
        val frame = AsciiFrame(96, 30)
        val pose = lookAt(WorldPosition(2.5, 2.5), definition.second.position)
        assertFailsWith<IllegalArgumentException> { system.render(frame, pose, metrics(frame, pose)) }
    }

    @Test
    fun `steady npc rendering allocates no per frame objects`() {
        val allocationBean = ManagementFactory.getThreadMXBean() as? com.sun.management.ThreadMXBean ?: return
        if (!allocationBean.isThreadAllocatedMemorySupported) return
        allocationBean.isThreadAllocatedMemoryEnabled = true
        val rin = content.npcs.single()
        val entity = requireNotNull(content.world.entity(rin.entityId))
        val system = NpcSystem(content.world, content.npcs, content.sprites, EntitySpriteRenderer())
        val pose = lookAt(WorldPosition(43.5, 16.5), entity.position)
        val frame = AsciiFrame(96, 30)
        val metrics = metrics(frame, pose)
        repeat(1_000) {
            frame.clear()
            system.render(frame, pose, metrics)
        }

        val threadId = Thread.currentThread().threadId()
        val before = allocationBean.getThreadAllocatedBytes(threadId)
        repeat(3_600) {
            frame.clear()
            system.render(frame, pose, metrics)
        }
        val allocatedBytes = allocationBean.getThreadAllocatedBytes(threadId) - before

        assertTrue(allocatedBytes <= 4_096, "steady NPC rendering allocated $allocatedBytes bytes")
    }

    private fun npc(
        id: String,
        displayName: String,
        dialogueId: String,
        position: WorldPosition,
    ): Pair<Npc, WorldEntity> {
        val entityId = EntityId(id)
        return Npc(
            entityId = entityId,
            spriteId = "sprite.rin",
            facingDegrees = 0.0,
            displayName = displayName,
            dialogueId = dialogueId,
        ) to WorldEntity(
            id = entityId,
            type = WorldEntityType.NPC,
            position = position,
            blocking = true,
            interactionId = dialogueId,
            displayText = displayName,
            collisionRadius = 0.24,
        )
    }

    private fun lookAt(from: WorldPosition, target: WorldPosition): ViewPose = ViewPose(
        position = from,
        eyeHeight = 0.60,
        yawRadians = ViewPose.normalizeYaw(atan2(target.x - from.x, target.z - from.z)),
        pitchRadians = 0.0,
        horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
    )

    private fun metrics(frame: AsciiFrame, pose: ViewPose) = ProjectionMetrics(
        columns = frame.width,
        rows = frame.height,
        pixelWidth = frame.width * FontMetrics.CELL_WIDTH,
        pixelHeight = frame.height * FontMetrics.CELL_HEIGHT,
        horizontalFovRadians = pose.horizontalFovRadians,
        pitchRadians = pose.pitchRadians,
    )

    companion object {
        private const val FIVE_MINUTE_FRAME_COUNT = 18_000
        private const val PHASE_FRAME_COUNT = FIVE_MINUTE_FRAME_COUNT / 4
    }
}
