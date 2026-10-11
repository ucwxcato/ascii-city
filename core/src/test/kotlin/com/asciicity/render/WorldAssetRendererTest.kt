package com.asciicity.render

import com.asciicity.TestContent
import com.asciicity.world.WorldAsset
import com.asciicity.world.WorldPosition
import kotlin.test.Test
import kotlin.test.assertTrue

class WorldAssetRendererTest {
    @Test
    fun `placed Unity model profile renders as a depth-tested ASCII sprite`() {
        val content = TestContent.loaded
        val sprite = content.sprites.sprite("sprite.rin")
        val asset = WorldAsset(
            id = "placement.test-tree",
            assetId = "tree.test",
            spriteId = sprite.id,
            position = WorldPosition(0.0, 5.0),
            anchorHeight = 0.0,
            yawRadians = 0.0,
            scaleX = 1.0,
            scaleY = 1.0,
            scaleZ = 1.0,
            blocking = false,
            collisionRadius = 0.0,
        )
        val frame = AsciiFrame(96, 30)
        frame.clear()
        val pose = ViewPose(
            position = WorldPosition(0.0, 0.0),
            eyeHeight = 0.6,
            yawRadians = 0.0,
            pitchRadians = 0.0,
            horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
        )
        val metrics = ProjectionMetrics(
            columns = frame.width,
            rows = frame.height,
            pixelWidth = frame.width * FontMetrics.CELL_WIDTH,
            pixelHeight = frame.height * FontMetrics.CELL_HEIGHT,
            horizontalFovRadians = pose.horizontalFovRadians,
            pitchRadians = pose.pitchRadians,
        )

        val written = WorldAssetRenderer(listOf(asset), content.sprites).render(frame, pose, metrics)

        assertTrue(written > 0)
        assertTrue((0 until frame.height).any { y ->
            (0 until frame.width).any { x -> frame.depthAt(x, y) == 5.0f }
        })
    }
}
