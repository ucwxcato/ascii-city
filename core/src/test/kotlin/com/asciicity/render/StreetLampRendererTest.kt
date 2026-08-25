package com.asciicity.render

import com.asciicity.TestContent
import com.asciicity.entity.WorldEntity
import com.asciicity.entity.WorldEntityType
import com.asciicity.world.EntityId
import com.asciicity.world.WorldPosition
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreetLampRendererTest {
    private val palette = TestContent.loaded.palette
    private val pose = ViewPose(
        position = WorldPosition(1.5, 1.5),
        eyeHeight = 0.6,
        yawRadians = 0.0,
        pitchRadians = 0.0,
        horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
    )
    private val metrics = ProjectionMetrics(96, 30, 960, 540, ViewPose.DEFAULT_FOV_RADIANS, 0.0)
    private val lamp = WorldEntity(
        id = EntityId("lamp.test"),
        type = WorldEntityType.LAMP,
        position = WorldPosition(1.5, 7.5),
        blocking = false,
        interactionId = null,
        displayText = null,
        collisionRadius = 0.0,
    )

    @Test
    fun `lamp renders a depth-tested post and local glow`() {
        val frame = AsciiFrame(96, 30)
        frame.clear(' '.code, palette[PaletteKeys.FOG], palette[PaletteKeys.SKY])

        StreetLampRenderer(listOf(lamp), palette).render(frame, pose, metrics)

        assertTrue(frameContains(frame, 'o'.code))
        assertTrue(frameContains(frame, '|'.code))
        assertTrue(frameContainsEmissive(frame))
        assertTrue(
            (0 until frame.height).any { row ->
                (0 until frame.width).any { column ->
                    frame.backgroundAt(column, row) != palette[PaletteKeys.SKY]
                }
            },
        )
    }

    @Test
    fun `nearer geometry occludes lamp glyphs and glow`() {
        val frame = AsciiFrame(96, 30)
        frame.clear(' '.code, palette[PaletteKeys.FOG], palette[PaletteKeys.SKY], depth = 1.0f)

        StreetLampRenderer(listOf(lamp), palette).render(frame, pose, metrics)

        assertFalse(frameContains(frame, 'o'.code))
        assertFalse(frameContains(frame, '|'.code))
        assertFalse(frameContainsEmissive(frame))
        assertTrue(
            (0 until frame.height).all { row ->
                (0 until frame.width).all { column ->
                    frame.backgroundAt(column, row) == palette[PaletteKeys.SKY]
                }
            },
        )
    }

    @Test
    fun `roadside layout places lamps across the complete road network`() {
        val content = TestContent.loaded
        val renderer = StreetLampRenderer(emptyList(), content.world.grid, content.palette)

        assertTrue(renderer.lampCount >= 80)
    }

    private fun frameContains(frame: AsciiFrame, glyph: Int): Boolean =
        (0 until frame.height).any { row -> (0 until frame.width).any { column -> frame.glyphAt(column, row) == glyph } }

    private fun frameContainsEmissive(frame: AsciiFrame): Boolean =
        (0 until frame.height).any { row -> (0 until frame.width).any { column -> frame.emissiveAt(column, row) > 0f } }
}
