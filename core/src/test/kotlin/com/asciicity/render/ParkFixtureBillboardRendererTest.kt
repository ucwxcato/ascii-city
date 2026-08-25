package com.asciicity.render

import com.asciicity.TestContent
import com.asciicity.content.CellRegistry
import com.asciicity.world.CellDefinition
import com.asciicity.world.WorldGrid
import com.asciicity.world.WorldPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParkFixtureBillboardRendererTest {
    @Test
    fun `bench silhouette stays identical when the camera and fixture rotate together`() {
        val northFrame = emptyFrame()
        renderer(fixtureGrid(width = 3, depth = 8, fixtureX = 1, fixtureZ = 5)).render(
            northFrame,
            pose(yawDegrees = 0.0),
            metrics(yawDegrees = 0.0),
            timeSeconds = 0.0,
        )

        val eastFrame = emptyFrame()
        renderer(fixtureGrid(width = 8, depth = 3, fixtureX = 5, fixtureZ = 1)).render(
            eastFrame,
            pose(yawDegrees = 90.0),
            metrics(yawDegrees = 90.0),
            timeSeconds = 0.0,
        )

        for (row in 0 until FRAME_HEIGHT) {
            for (column in 0 until FRAME_WIDTH) {
                assertEquals(northFrame.glyphAt(column, row), eastFrame.glyphAt(column, row))
                assertEquals(northFrame.foregroundAt(column, row), eastFrame.foregroundAt(column, row))
                assertEquals(northFrame.backgroundAt(column, row), eastFrame.backgroundAt(column, row))
                assertEquals(northFrame.depthAt(column, row), eastFrame.depthAt(column, row))
            }
        }
    }

    @Test
    fun `slot fixture renders a recognizable cabinet instead of an opaque block`() {
        val frame = emptyFrame()
        renderer(slotGrid()).render(
            frame,
            pose(yawDegrees = 0.0),
            metrics(yawDegrees = 0.0),
            timeSeconds = 1.0,
        )

        val glyphs = buildSet {
            for (row in 0 until FRAME_HEIGHT) for (column in 0 until FRAME_WIDTH) {
                add(frame.glyphAt(column, row).toChar())
            }
        }
        assertTrue(setOf('#', '|', 'o', '$', '*').all { it in glyphs })
        assertTrue(glyphs.any { it in "7BAR" })
        assertTrue(
            (0 until FRAME_HEIGHT).any { row ->
                (0 until FRAME_WIDTH).any { column -> frame.emissiveAt(column, row) > 0f }
            },
        )
    }

    @Test
    fun `slot fixture gains a partial cabinet fill while silhouette gaps stay transparent`() {
        val frame = emptyFrame()
        renderer(slotGrid()).render(
            frame,
            pose(yawDegrees = 0.0),
            metrics(yawDegrees = 0.0),
            timeSeconds = 1.0,
        )

        val palette = TestContent.loaded.palette
        val sky = palette[PaletteKeys.SKY]
        val cabinet = palette["casinoCabinet"]
        val filledCells = buildList {
            for (row in 0 until FRAME_HEIGHT) for (column in 0 until FRAME_WIDTH) {
                if (frame.depthAt(column, row).isFinite()) add(column to row)
            }
        }

        assertTrue(filledCells.isNotEmpty())
        assertTrue(filledCells.all { (column, row) -> frame.backgroundAt(column, row) != sky })
        assertTrue(filledCells.all { (column, row) ->
            (frame.backgroundAt(column, row) ushr 24 and 0xFF) < (cabinet ushr 24 and 0xFF) / 2
        })
        val columns = filledCells.map { it.first }
        val rows = filledCells.map { it.second }
        assertTrue((rows.min()..rows.max()).any { row ->
            (columns.min()..columns.max()).any { column ->
                frame.depthAt(column, row).isInfinite() && frame.backgroundAt(column, row) == sky
            }
        })
    }

    @Test
    fun `slot cabinet fill fades out before the distant silhouette compacts`() {
        fun renderedBackgroundAt(fixtureZ: Int, gridDepth: Int): Int {
            val frame = emptyFrame()
            renderer(slotGrid(fixtureZ, gridDepth)).render(
                frame,
                pose(yawDegrees = 0.0),
                metrics(yawDegrees = 0.0),
                timeSeconds = 1.0,
            )
            for (row in 0 until FRAME_HEIGHT) for (column in 0 until FRAME_WIDTH) {
                if (frame.depthAt(column, row).isFinite()) return frame.backgroundAt(column, row)
            }
            error("Slot fixture did not render at z=$fixtureZ")
        }

        val close = renderedBackgroundAt(fixtureZ = 5, gridDepth = 8)
        val middle = renderedBackgroundAt(fixtureZ = 10, gridDepth = 13)
        val distant = renderedBackgroundAt(fixtureZ = 15, gridDepth = 18)
        val closeRed = close ushr 24 and 0xFF
        val middleRed = middle ushr 24 and 0xFF

        assertTrue(closeRed > middleRed)
        assertTrue(middleRed > (distant ushr 24 and 0xFF))
        assertEquals(TestContent.loaded.palette[PaletteKeys.SKY], distant)
    }

    private fun renderer(grid: WorldGrid) = ParkFixtureBillboardRenderer(
        grid,
        TestContent.loaded.materials,
        MaterialSampler(TestContent.loaded.palette, TestContent.loaded.presentation),
        maxDrawDistance = 48.0,
    )

    private fun fixtureGrid(width: Int, depth: Int, fixtureX: Int, fixtureZ: Int): WorldGrid {
        val definitions = listOf(
            CellDefinition("test.road", '.', "ground.road", "facade.none", false, false, 0, emptySet(), null),
            CellDefinition(
                "test.bench",
                'b',
                "ground.park",
                "facade.park-wood",
                true,
                true,
                1,
                setOf("park", "bench", "fixture"),
                null,
            ),
        )
        val indices = IntArray(width * depth)
        indices[fixtureZ * width + fixtureX] = 1
        return WorldGrid.fromResolvedIndices(
            width,
            depth,
            CellRegistry.create(definitions, TestContent.loaded.materials, "test"),
            indices,
        )
    }

    private fun slotGrid(fixtureZ: Int = 5, depth: Int = 8): WorldGrid {
        val definitions = listOf(
            CellDefinition("test.road", '.', "ground.road", "facade.none", false, false, 0, emptySet(), null),
            CellDefinition(
                "test.slot",
                's',
                "ground.casino-carpet",
                "facade.casino-slot",
                true,
                true,
                2,
                setOf("interior", "casino", "slot-machine", "fixture"),
                null,
            ),
        )
        val indices = IntArray(3 * depth)
        indices[fixtureZ * 3 + 1] = 1
        return WorldGrid.fromResolvedIndices(
            3,
            depth,
            CellRegistry.create(definitions, TestContent.loaded.materials, "test"),
            indices,
        )
    }

    private fun emptyFrame() = AsciiFrame(FRAME_WIDTH, FRAME_HEIGHT).apply {
        clear(
            glyph = '.'.code,
            foreground = TestContent.loaded.palette[PaletteKeys.FOG],
            background = TestContent.loaded.palette[PaletteKeys.SKY],
        )
    }

    private fun pose(yawDegrees: Double) = ViewPose(
        position = WorldPosition(1.5, 1.5),
        eyeHeight = 0.6,
        yawRadians = Math.toRadians(yawDegrees),
        pitchRadians = 0.0,
        horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
    )

    private fun metrics(yawDegrees: Double): ProjectionMetrics {
        val pose = pose(yawDegrees)
        return ProjectionMetrics(
            columns = FRAME_WIDTH,
            rows = FRAME_HEIGHT,
            pixelWidth = FRAME_WIDTH * FontMetrics.CELL_WIDTH,
            pixelHeight = FRAME_HEIGHT * FontMetrics.CELL_HEIGHT,
            horizontalFovRadians = pose.horizontalFovRadians,
            pitchRadians = pose.pitchRadians,
        )
    }

    private companion object {
        const val FRAME_WIDTH = 97
        const val FRAME_HEIGHT = 31
    }
}
