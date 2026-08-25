package com.asciicity.render

import com.asciicity.TestContent
import com.asciicity.world.WorldPosition
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EntitySpriteRendererTest {
    private val content = TestContent.loaded
    private val sprite = content.sprites.sprite("sprite.rin")
    private val renderer = EntitySpriteRenderer()
    private val pose = ViewPose(
        position = WorldPosition(0.0, 0.0),
        eyeHeight = 0.6,
        yawRadians = 0.0,
        pitchRadians = 0.0,
        horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
    )
    private val metrics = ProjectionMetrics(
        columns = FRAME_WIDTH,
        rows = FRAME_HEIGHT,
        pixelWidth = FRAME_WIDTH * FontMetrics.CELL_WIDTH,
        pixelHeight = FRAME_HEIGHT * FontMetrics.CELL_HEIGHT,
        horizontalFovRadians = pose.horizontalFovRadians,
        pitchRadians = pose.pitchRadians,
    )

    @Test
    fun `near sprite is larger while behind and over-distance sprites are rejected`() {
        val nearFrame = emptyFrame()
        val farFrame = emptyFrame()
        val nearWrites = renderer.render(nearFrame, pose, metrics, sprite, WorldPosition(0.0, 5.0))
        val farWrites = renderer.render(farFrame, pose, metrics, sprite, WorldPosition(0.0, 15.0))

        assertTrue(nearWrites > farWrites)
        assertTrue(farWrites > 0)
        assertEquals(0, renderer.render(emptyFrame(), pose, metrics, sprite, WorldPosition(0.0, -5.0)))
        assertEquals(0, renderer.render(emptyFrame(), pose, metrics, sprite, WorldPosition(0.0, 49.0)))
        assertEquals(0, renderer.render(emptyFrame(), pose, metrics, sprite, WorldPosition(20.0, 5.0)))
    }

    @Test
    fun `camera-facing projection is stable when view and placement rotate together`() {
        val northFrame = emptyFrame()
        renderer.render(northFrame, pose, metrics, sprite, WorldPosition(0.0, 5.0))

        val eastPose = pose.copy(yawRadians = Math.toRadians(90.0))
        val eastMetrics = ProjectionMetrics(
            columns = FRAME_WIDTH,
            rows = FRAME_HEIGHT,
            pixelWidth = FRAME_WIDTH * FontMetrics.CELL_WIDTH,
            pixelHeight = FRAME_HEIGHT * FontMetrics.CELL_HEIGHT,
            horizontalFovRadians = eastPose.horizontalFovRadians,
            pitchRadians = eastPose.pitchRadians,
        )
        val eastFrame = emptyFrame()
        renderer.render(eastFrame, eastPose, eastMetrics, sprite, WorldPosition(5.0, 0.0))

        assertEquals(frameHash(northFrame), frameHash(eastFrame))
    }

    @Test
    fun `depth is compared independently for every visible character`() {
        val unobstructed = emptyFrame()
        val unobstructedWrites = renderer.render(
            unobstructed,
            pose,
            metrics,
            sprite,
            WorldPosition(0.0, 5.0),
        )
        assertTrue(unobstructedWrites > 0)
        assertTrue(frameContainsDepth(unobstructed, 5.0f))

        val fullyOccluded = wallFrame(0)
        assertEquals(
            0,
            renderer.render(fullyOccluded, pose, metrics, sprite, WorldPosition(0.0, 5.0)),
        )

        val partiallyOccluded = wallFrame(FRAME_WIDTH / 2 + 1)
        val partialWrites = renderer.render(
            partiallyOccluded,
            pose,
            metrics,
            sprite,
            WorldPosition(0.0, 5.0),
        )
        assertTrue(partialWrites in 1 until unobstructedWrites)
        assertTrue(frameContainsDepth(partiallyOccluded, WALL_DEPTH))
        assertTrue(frameContainsDepth(partiallyOccluded, 5.0f))
    }

    @Test
    fun `near far edge behind-wall and partial-occlusion fixtures reproduce exact hashes`() {
        val fixtures = linkedMapOf(
            "near" to emptyFrame().also {
                renderer.render(it, pose, metrics, sprite, WorldPosition(0.0, 5.0))
            },
            "far" to emptyFrame().also {
                renderer.render(it, pose, metrics, sprite, WorldPosition(0.0, 15.0))
            },
            "edge" to emptyFrame().also {
                renderer.render(it, pose, metrics, sprite, WorldPosition(3.7, 5.0))
            },
            "behind-wall" to wallFrame(0).also {
                renderer.render(it, pose, metrics, sprite, WorldPosition(0.0, 5.0))
            },
            "partial-occlusion" to wallFrame(FRAME_WIDTH / 2 + 1).also {
                renderer.render(it, pose, metrics, sprite, WorldPosition(0.0, 5.0))
            },
        )
        val actual = fixtures.mapValues { (_, frame) -> frameHash(frame) }
        val expected = mapOf(
            "near" to "dbb82650974ddc86e0c8b153a4bc237fff0686d26427553a6a1edadc72a8b912",
            "far" to "5bc71e663083e13db1d5d67e86ef89deb16533143d76293367436df9a4a6c67e",
            "edge" to "27cc6de2898f9dd038a254cc8b9ef09bc20cf4b964ea2b337a5fe5a38be8c47d",
            "behind-wall" to "dc845e332a86afc7de68f828a2067a785a5d8158599b93d568b428966748d9f6",
            "partial-occlusion" to "58e2b74c7888316ff6d196d435af6dd8c00ae22e8664d378ca2e343b1737a51d",
        )

        assertEquals(
            expected,
            actual,
            fixtures.entries.joinToString(separator = "\n\n") { (name, frame) ->
                "$name:\n${framePreview(frame)}"
            },
        )
    }

    private fun emptyFrame(): AsciiFrame = AsciiFrame(FRAME_WIDTH, FRAME_HEIGHT).apply {
        clear(
            glyph = '.'.code,
            foreground = content.palette[PaletteKeys.FOG],
            background = content.palette[PaletteKeys.SKY],
        )
    }

    private fun wallFrame(firstWallColumn: Int): AsciiFrame = emptyFrame().apply {
        var row = 0
        while (row < height) {
            var column = firstWallColumn
            while (column < width) {
                set(
                    x = column,
                    y = row,
                    glyph = '#'.code,
                    foreground = content.palette[PaletteKeys.WALL_FRONT],
                    background = content.palette[PaletteKeys.WALL_SIDE],
                    depth = WALL_DEPTH,
                )
                column += 1
            }
            row += 1
        }
    }

    private fun frameContainsDepth(frame: AsciiFrame, depth: Float): Boolean =
        (0 until frame.height).any { row ->
            (0 until frame.width).any { column -> frame.depthAt(column, row) == depth }
        }

    private fun frameHash(frame: AsciiFrame): String {
        val digest = MessageDigest.getInstance("SHA-256")
        for (row in 0 until frame.height) {
            for (column in 0 until frame.width) {
                digest.updateInt(frame.glyphAt(column, row))
                digest.updateInt(frame.foregroundAt(column, row))
                digest.updateInt(frame.backgroundAt(column, row))
                digest.updateInt(frame.depthAt(column, row).toBits())
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun framePreview(frame: AsciiFrame): String = buildString {
        for (row in frame.height - 1 downTo 0) {
            for (column in 0 until frame.width) append(frame.glyphAt(column, row).toChar())
            if (row > 0) append('\n')
        }
    }

    private fun MessageDigest.updateInt(value: Int) {
        update((value ushr 24).toByte())
        update((value ushr 16).toByte())
        update((value ushr 8).toByte())
        update(value.toByte())
    }

    companion object {
        private const val FRAME_WIDTH = 97
        private const val FRAME_HEIGHT = 31
        private const val WALL_DEPTH = 2.0f
    }
}
