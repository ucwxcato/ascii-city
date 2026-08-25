package com.asciicity.render

import com.asciicity.TestContent
import com.asciicity.world.WorldPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AtmosphericRendererTest {
    @Test
    fun `steam sampling repeats and cannot overwrite nearer geometry`() {
        val renderer = AtmosphericRenderer(
            listOf(
                AtmosphericEmitter(
                    "steam.test",
                    AtmosphericEmitterKind.STEAM,
                    WorldPosition(2.5, 4.5),
                    0.05,
                    1.8,
                    0.2,
                    3.5,
                    0.25,
                ),
            ),
            MaterialSampler(TestContent.loaded.palette),
        )
        val pose = ViewPose(
            WorldPosition(2.5, 1.5),
            0.6,
            0.0,
            0.0,
            ViewPose.DEFAULT_FOV_RADIANS,
        )
        val first = AsciiFrame(96, 30)
        val repeated = AsciiFrame(96, 30)
        val metrics = ProjectionMetrics(96, 30, 960, 540, pose.horizontalFovRadians, pose.pitchRadians)

        val firstWrites = renderer.render(first, pose, metrics, 2.75)
        val repeatedWrites = renderer.render(repeated, pose, metrics, 2.75)
        assertTrue(firstWrites >= 8, "close steam should produce a readable fixture and multi-glyph wisps")
        assertTrue(
            (0 until first.height).any { y -> (0 until first.width).any { x -> first.glyphAt(x, y) == '#'.code } },
            "street steam must retain a visible grate between wisps",
        )
        assertEquals(firstWrites, repeatedWrites)
        for (y in 0 until first.height) for (x in 0 until first.width) {
            assertEquals(first.glyphAt(x, y), repeated.glyphAt(x, y))
            assertEquals(first.depthAt(x, y), repeated.depthAt(x, y))
        }

        repeated.clear(depth = 1f)
        assertEquals(0, renderer.render(repeated, pose, metrics, 2.75))
    }
}
