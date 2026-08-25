package com.asciicity.ambient

import com.asciicity.TestContent
import com.asciicity.render.AsciiFrame
import com.asciicity.render.FontMetrics
import com.asciicity.render.MaterialSampler
import com.asciicity.render.ProjectionMetrics
import com.asciicity.render.ViewPose
import com.asciicity.world.WorldPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AmbientEventSystemTest {
    @Test
    fun `event intervals are deterministic and spatial playback is distance attenuated and panned`() {
        val emitter = AmbientEventEmitter(
            id = "event.test-spark",
            kind = AmbientEventKind.ELECTRIC_SPARK,
            position = WorldPosition(3.5, 4.5),
            height = 1.2,
            minimumIntervalSeconds = 2.0,
            maximumIntervalSeconds = 2.0,
            durationSeconds = 0.5,
            audibleRange = 10.0,
        )
        val first = AmbientEventSystem(listOf(emitter))
        val second = AmbientEventSystem(listOf(emitter))
        val pose = pose()
        val playback = mutableListOf<AmbientEventPlayback>()

        assertEquals(first.nextEventSeconds(0), second.nextEventSeconds(0))
        first.step(1.99, pose, playback::add)
        assertFalse(first.isActive(0))
        first.step(0.01, pose, playback::add)

        assertTrue(first.isActive(0))
        assertEquals(1, playback.size)
        assertTrue(playback.single().volume in 0f..<1f)
        assertTrue(playback.single().pan > 0f)
        assertTrue(playback.single().pitch in 0.92f..1.08f)
    }

    @Test
    fun `active event glyph uses finite depth and respects nearer frame content`() {
        val emitter = AmbientEventEmitter(
            "event.test-vent",
            AmbientEventKind.VENT_HISS,
            WorldPosition(2.5, 4.5),
            0.5,
            2.0,
            2.0,
            0.5,
            8.0,
        )
        val system = AmbientEventSystem(listOf(emitter))
        val pose = pose()
        val frame = AsciiFrame(96, 30)
        val metrics = metrics(frame, pose)
        system.step(2.0, pose)

        assertEquals(1, system.render(frame, pose, metrics, MaterialSampler(TestContent.loaded.palette)))
        val visibleDepths = (0 until frame.height).flatMap { y ->
            (0 until frame.width).map { x -> frame.depthAt(x, y) }
        }.filter(Float::isFinite)
        assertEquals(1, visibleDepths.size)

        frame.clear(depth = 1f)
        assertEquals(0, system.render(frame, pose, metrics, MaterialSampler(TestContent.loaded.palette)))
    }

    private fun pose() = ViewPose(
        position = WorldPosition(2.5, 2.5),
        eyeHeight = 0.6,
        yawRadians = 0.0,
        pitchRadians = 0.0,
        horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
    )

    private fun metrics(frame: AsciiFrame, pose: ViewPose) = ProjectionMetrics(
        frame.width,
        frame.height,
        frame.width * FontMetrics.CELL_WIDTH,
        frame.height * FontMetrics.CELL_HEIGHT,
        pose.horizontalFovRadians,
        pose.pitchRadians,
    )
}
