package com.asciicity.render

import com.asciicity.TestContent
import com.asciicity.world.WorldPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TerminalBillboardRendererTest {
    private val content = TestContent.loaded
    private val pose = ViewPose(
        position = WorldPosition(51.5, 19.2),
        eyeHeight = content.world.player.eyeHeight,
        yawRadians = 0.0,
        pitchRadians = 0.0,
        horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
    )

    @Test
    fun `archive terminal renders a visible screen billboard at its authoritative world position`() {
        val frame = AsciiFrame(128, 40)
        val renderer = TerminalBillboardRenderer(content.world, content.sprites)

        val written = renderer.render(frame, pose, metrics(frame))

        assertTrue(written > 0)
        assertTrue((0 until frame.height).any { row ->
            (0 until frame.width).any { column -> frame.depthAt(column, row) == 1.3f }
        })
        assertEquals("sprite.terminal", content.sprites.sprite(TerminalBillboardRenderer.TERMINAL_SPRITE_ID).id)
    }

    @Test
    fun `world renderer composes the terminal billboard after opaque world geometry`() {
        val frame = AsciiFrame(128, 40)
        val renderer = WorldAsciiRenderer(
            world = content.world,
            materials = content.materials,
            palette = content.palette,
            presentation = content.presentation,
            facadeSigns = content.facadeSigns,
            terminalBillboardRenderer = TerminalBillboardRenderer(content.world, content.sprites),
        )

        renderer.render(frame, pose, 4.0)

        assertTrue(renderer.stats.terminalSpriteWrites > 0)
        assertTrue((0 until frame.height).any { row ->
            (0 until frame.width).any { column -> frame.depthAt(column, row) == 1.3f }
        })
    }

    private fun metrics(frame: AsciiFrame) = ProjectionMetrics(
        columns = frame.width,
        rows = frame.height,
        pixelWidth = frame.width * FontMetrics.CELL_WIDTH,
        pixelHeight = frame.height * FontMetrics.CELL_HEIGHT,
        horizontalFovRadians = pose.horizontalFovRadians,
        pitchRadians = pose.pitchRadians,
    )
}
