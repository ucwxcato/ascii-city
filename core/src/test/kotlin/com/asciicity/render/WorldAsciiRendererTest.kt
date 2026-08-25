package com.asciicity.render

import com.asciicity.TestContent
import com.asciicity.content.CellRegistry
import com.asciicity.content.ContentLoader
import com.asciicity.entity.PlayerState
import com.asciicity.system.NpcSystem
import com.asciicity.world.CellDefinition
import com.asciicity.world.GameWorld
import com.asciicity.world.WorldGrid
import com.asciicity.world.WorldPosition
import java.security.MessageDigest
import java.lang.management.ManagementFactory
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorldAsciiRendererTest {
    @Test
    fun `taller distant wall remains visible only above shorter near wall`() {
        val shortNear = testRenderer(nearHeight = 1, farHeight = 3)
        val frame = AsciiFrame(101, 40)
        shortNear.render(frame, testPose(), 2.0)
        val depths = (0 until frame.height).map { frame.depthAt(50, it) }
        assertTrue(depths.any { it in 1.49f..1.51f })
        assertTrue(depths.any { it in 3.49f..3.51f })

        val tallNear = testRenderer(nearHeight = 3, farHeight = 1)
        tallNear.render(frame, testPose(), 2.0)
        val reverseDepths = (0 until frame.height).map { frame.depthAt(50, it) }
        assertTrue(reverseDepths.any { it in 1.49f..1.51f })
        assertFalse(reverseDepths.any { it in 3.49f..3.51f })
    }

    @Test
    fun `equal walls gaps and maximum distance obey depth rules`() {
        val frame = AsciiFrame(101, 40)
        testRenderer(nearHeight = 2, farHeight = 2).render(frame, testPose(), 2.0)
        assertTrue((0 until frame.height).any { frame.depthAt(50, it) in 1.49f..1.51f })
        assertFalse((0 until frame.height).any { frame.depthAt(50, it) in 3.49f..3.51f })

        testRenderer(nearHeight = null, farHeight = 3).render(frame, testPose(), 2.0)
        assertTrue((0 until frame.height).any { frame.depthAt(50, it) in 3.49f..3.51f })

        testRenderer(nearHeight = null, farHeight = 3, maxDistance = 2.0).render(frame, testPose(), 2.0)
        assertFalse((0 until frame.height).any { frame.depthAt(50, it) in 3.49f..3.51f })
    }

    @Test
    fun `city frame is deterministic animated and preserves geometry`() {
        val content = TestContent.loaded
        val renderer = cityRenderer()
        val pose = cityPose(0.0, 0.0)
        val first = AsciiFrame(96, 30)
        val repeated = AsciiFrame(96, 30)
        val advanced = AsciiFrame(96, 30)

        renderer.render(first, pose, 3.0)
        renderer.render(repeated, pose, 3.0)
        renderer.render(advanced, pose, 3.25)

        assertTrue(frameHash(first).contentEquals(frameHash(repeated)))
        assertFalse(frameHash(first).contentEquals(frameHash(advanced)))
        assertFalse(depthHash(first).contentEquals(depthHash(advanced)), "fireflies animate at finite world depth")
        assertEquals(96, renderer.stats.rayCount)
        assertTrue(renderer.stats.ddaSteps > 0)
        assertTrue(renderer.stats.wallHits > 0)
        assertTrue(renderer.stats.filledGlyphCount > 0)
    }

    @Test
    fun `dense profile increases ray sampling without changing world-space framing`() {
        val content = TestContent.loaded
        val renderer = cityRenderer(AsciiFontProfile.DENSE)
        val frame = AsciiFrame(160, 48)
        val pose = cityPose(0.0, 0.0)

        renderer.render(frame, pose, 3.0)

        assertEquals(160, renderer.stats.rayCount)
        assertTrue(renderer.stats.filledGlyphCount > 0)
        val standardMetrics = ProjectionMetrics(128, 40, 1280, 720, pose.horizontalFovRadians, pose.pitchRadians)
        val denseMetrics = ProjectionMetrics(160, 48, 1280, 720, pose.horizontalFovRadians, pose.pitchRadians)
        assertEquals(standardMetrics.effectiveHorizontalFovRadians, denseMetrics.effectiveHorizontalFovRadians, 1e-12)
        assertEquals(standardMetrics.verticalFovRadians, denseMetrics.verticalFovRadians, 1e-12)
    }

    @Test
    fun `water park ceiling fills the upper view and keeps rain outside`() {
        val content = ContentLoader().load(Path.of("."), "water-park")
        val renderer = WorldAsciiRenderer(
            content.world,
            content.materials,
            content.palette,
            content.presentation,
            ceiling = requireNotNull(content.metadata.ceiling),
        )
        val player = content.world.player
        val pose = ViewPose(
            position = player.position,
            eyeHeight = player.eyeHeight,
            yawRadians = player.yawRadians,
            pitchRadians = Math.toRadians(10.0),
            horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
        )
        val frame = AsciiFrame(128, 40)

        renderer.render(frame, pose, 0.0)

        assertTrue(renderer.stats.ceilingWrites > 0)
        for (column in 0 until frame.width) {
            assertTrue(frame.depthAt(column, frame.height - 1).isFinite(), "top row $column must be enclosed")
            assertFalse(
                frame.foregroundAt(column, frame.height - 1) == content.palette[PaletteKeys.RAIN],
                "rain must not overwrite the ceiling at top-row column $column",
            )
        }
        assertEquals(
            "a0feb893db67120f8db65e4b7545b707423967700d2b6b2a0308769bd7396535",
            frameHash(frame).toHex(),
        )
    }

    @Test
    fun `selected city poses reproduce exact frame hashes`() {
        val content = TestContent.loaded
        val renderer = cityRenderer()
        val frame = AsciiFrame(96, 30)
        val fixtures = linkedMapOf(
            "north-level" to cityPose(0.0, 0.0),
            "east-level" to cityPose(90.0, 0.0),
            "south-level" to cityPose(180.0, 0.0),
            "west-level" to cityPose(270.0, 0.0),
            "diagonal-up" to cityPose(45.0, 25.0),
            "diagonal-down" to cityPose(225.0, -25.0),
            "rin-approach" to rinApproachPose(),
        )
        val actual = fixtures.mapValues { (_, pose) ->
            renderer.render(frame, pose, 4.0)
            frameHash(frame).toHex()
        }
        val expected = mapOf(
            "north-level" to "fb878cf1f010b294e1fcc5bd6dc031cf97b38565887c69b78d2401636aff0d57",
            "east-level" to "f079484050f2db9a3ebbb0beed0a85dd7aa534284994eb2d585e3a2630cd0b84",
            "south-level" to "b7941377751058fb5d14fe5cfcc9f1fd699badfc0bb44780e3bf2dbfdbb2547d",
            "west-level" to "5a1dd03192249404433a57cbfc13307d416e76866e51daccdcfeaa0340a44bfd",
            "diagonal-up" to "9029c744f2b2b0f97f108dc9eedd38e712d9f844d462c618d4b18ace2bb65e55",
            "diagonal-down" to "42487a5882781c56ebd2df45bfd635a80e805a4ec3a69b7edbe0671938e53cc9",
            "rin-approach" to "3ed58ee0afb644a421c637949a4f45f36889ce2ff03f44b157aae5b6a5c1e5e5",
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `sixty simulated seconds allocate no per ray or glyph objects`() {
        val allocationBean = ManagementFactory.getThreadMXBean() as? com.sun.management.ThreadMXBean ?: return
        if (!allocationBean.isThreadAllocatedMemorySupported) return
        allocationBean.isThreadAllocatedMemoryEnabled = true
        val content = TestContent.loaded
        val renderer = cityRenderer()
        val frame = AsciiFrame(96, 30)
        val pose = cityPose(0.0, 0.0)
        repeat(1_000) { renderer.render(frame, pose, 3.0) }

        val threadId = Thread.currentThread().threadId()
        val before = allocationBean.getThreadAllocatedBytes(threadId)
        repeat(3_600) { renderer.render(frame, pose, 3.0) }
        val allocatedBytes = allocationBean.getThreadAllocatedBytes(threadId) - before

        assertTrue(allocatedBytes <= 4_096, "steady renderer allocated $allocatedBytes bytes")
    }

    @Test
    fun `dense sixty simulated seconds allocate no per ray or glyph objects`() {
        val allocationBean = ManagementFactory.getThreadMXBean() as? com.sun.management.ThreadMXBean ?: return
        if (!allocationBean.isThreadAllocatedMemorySupported) return
        allocationBean.isThreadAllocatedMemoryEnabled = true
        val renderer = cityRenderer(AsciiFontProfile.DENSE)
        val frame = AsciiFrame(160, 48)
        val pose = cityPose(0.0, 0.0)
        repeat(1_000) { renderer.render(frame, pose, 3.0) }

        val threadId = Thread.currentThread().threadId()
        val before = allocationBean.getThreadAllocatedBytes(threadId)
        repeat(3_600) { renderer.render(frame, pose, 3.0) }
        val allocatedBytes = allocationBean.getThreadAllocatedBytes(threadId) - before

        assertTrue(allocatedBytes <= 4_096, "dense steady renderer allocated $allocatedBytes bytes")
    }

    private fun cityPose(yawDegrees: Double, pitchDegrees: Double): ViewPose {
        val player = TestContent.loaded.world.player
        return ViewPose(
            position = player.position,
            eyeHeight = player.eyeHeight,
            yawRadians = Math.toRadians(yawDegrees),
            pitchRadians = Math.toRadians(pitchDegrees),
            horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
        )
    }

    private fun rinApproachPose() = ViewPose(
        position = WorldPosition(43.5, 16.5),
        eyeHeight = TestContent.loaded.world.player.eyeHeight,
        yawRadians = Math.toRadians(90.0),
        pitchRadians = 0.0,
        horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
    )

    private fun cityRenderer(fontProfile: AsciiFontProfile = AsciiFontProfile.STANDARD): WorldAsciiRenderer {
        val content = TestContent.loaded
        return WorldAsciiRenderer(
            content.world,
            content.materials,
            content.palette,
            facadeSigns = content.facadeSigns,
            npcSystem = NpcSystem(content.world, content.npcs, content.sprites),
            fontProfile = fontProfile,
        )
    }

    private fun testPose() = ViewPose(
        position = WorldPosition(1.5, 1.5),
        eyeHeight = 0.6,
        yawRadians = 0.0,
        pitchRadians = 0.0,
        horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
    )

    private fun testRenderer(
        nearHeight: Int?,
        farHeight: Int?,
        maxDistance: Double = 8.0,
    ): WorldAsciiRenderer {
        val content = TestContent.loaded
        val definitions = listOf(
            CellDefinition("test.road", '.', "ground.road", "facade.none", false, false, 0, emptySet(), null),
            CellDefinition("test.near", 'n', "ground.road", "facade.low", true, true, nearHeight ?: 1, emptySet(), null),
            CellDefinition("test.far", 'f', "ground.road", "facade.tall", true, true, farHeight ?: 1, emptySet(), null),
        )
        val registry = CellRegistry.create(definitions, content.materials, "test")
        val indices = IntArray(3 * 8)
        if (nearHeight != null) indices[3 * 3 + 1] = 1
        if (farHeight != null) indices[5 * 3 + 1] = 2
        val grid = WorldGrid.fromResolvedIndices(3, 8, registry, indices)
        val player = PlayerState(WorldPosition(1.5, 1.5), 0.0)
        val world = GameWorld("test", 1, grid, player, emptyList())
        return WorldAsciiRenderer(world, content.materials, content.palette, maxRayDistance = maxDistance)
    }

    private fun frameHash(frame: AsciiFrame): ByteArray = digest(frame) { x, y, digest ->
        digest.updateInt(frame.glyphAt(x, y))
        digest.updateInt(frame.foregroundAt(x, y))
        digest.updateInt(frame.backgroundAt(x, y))
        digest.updateInt(frame.depthAt(x, y).toBits())
    }

    private fun depthHash(frame: AsciiFrame): ByteArray = digest(frame) { x, y, digest ->
        digest.updateInt(frame.depthAt(x, y).toBits())
    }

    private inline fun digest(
        frame: AsciiFrame,
        update: (Int, Int, MessageDigest) -> Unit,
    ): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        for (y in 0 until frame.height) for (x in 0 until frame.width) update(x, y, digest)
        return digest.digest()
    }

    private fun MessageDigest.updateInt(value: Int) {
        update((value ushr 24).toByte())
        update((value ushr 16).toByte())
        update((value ushr 8).toByte())
        update(value.toByte())
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
