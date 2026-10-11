package com.asciicity.render

import com.asciicity.TestContent
import com.asciicity.world.WorldPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FacadeLabelRendererTest {
    @Test
    fun `labels stay one compact glyph per character when a readable facade is close`() {
        val sign = sign()
        val frame = AsciiFrame(96, 30)
        frame.clear()
        val pose = pose(yawRadians = 0.0)

        val written = labelRenderer(sign).render(
            frame,
            pose,
            metrics(frame, pose),
        )

        assertEquals(sign.label!!.count { it != ' ' }, written)
        assertEquals(1, glyphRows(frame).sumOf { row -> row.count { it == 'C' } })
        assertEquals(1, glyphRows(frame).sumOf { row -> row.count { it == 'O' } })
        assertTrue(
            (0 until frame.height).any { row ->
                (0 until frame.width).any { column -> frame.emissiveAt(column, row) > 0f }
            },
        )
    }

    @Test
    fun `labels render in the center band of their sign`() {
        val sign = sign()
        val frame = AsciiFrame(96, 30)
        frame.clear()
        val pose = pose(yawRadians = 0.0)
        val metrics = metrics(frame, pose)

        labelRenderer(sign).render(frame, pose, metrics)

        val signDepth = 10.0
        val centerRow = kotlin.math.floor(
            metrics.projectWorldHeight(sign.bottom + sign.height * 0.55, pose.eyeHeight, signDepth),
        ).toInt()
        val labelRows = glyphRows(frame).flatMapIndexed { row, glyphs ->
            glyphs.filter { it in "CASINO" }.map { row }
        }
        assertTrue(labelRows.isNotEmpty())
        assertTrue(labelRows.all { it == centerRow })
    }

    @Test
    fun `labels do not draw through the back of their facade`() {
        val sign = sign()
        val frame = AsciiFrame(96, 30)
        frame.clear()
        val pose = pose(yawRadians = Math.PI)

        assertEquals(
            0,
            labelRenderer(sign).render(
                frame,
                pose,
                metrics(frame, pose),
            ),
        )
    }

    @Test
    fun `labels disappear at a shallow angle instead of facing the camera`() {
        val sign = sign()
        val frame = AsciiFrame(96, 30)
        frame.clear()
        val position = WorldPosition(38.0, -4.0)
        val pose = pose(
            position = position,
            yawRadians = kotlin.math.atan2(5.0 - position.x, 6.0 - position.z),
        )

        assertEquals(
            0,
            labelRenderer(sign).render(
                frame,
                pose,
                metrics(frame, pose),
            ),
        )
    }

    @Test
    fun `last light label remains visible on its facade at close walking distance`() {
        val sign = TestContent.loaded.facadeSigns.first { it.id == "sign.last-light-casino" }
        val frame = AsciiFrame(128, 40)
        frame.clear()
        val pose = ViewPose(
            position = WorldPosition(28.5, 8.5),
            eyeHeight = 0.6,
            yawRadians = Math.toRadians(180.0),
            pitchRadians = Math.toRadians(25.0),
            horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
        )

        assertTrue(
            labelRenderer(sign).render(
                frame,
                pose,
                metrics(frame, pose),
            ) > 0,
        )
    }

    private fun sign() = FacadeSign(
        id = "sign.label-test",
        face = FacadeFace.SOUTH,
        cellX = 4,
        cellZ = 6,
        widthCells = 2,
        bottom = 1.0,
        height = 2.0,
        glyphRows = listOf("---", "---"),
        distantGlyphRows = listOf("---", "---"),
        borderPaletteKey = PaletteKeys.SIGN_GOLD,
        textPaletteKey = PaletteKeys.SIGN_CREAM,
        accentPaletteKey = PaletteKeys.SIGN_RED_ORANGE,
        backgroundPaletteKey = PaletteKeys.SIGN_BACKGROUND,
        animationRate = 0.0,
        label = "CASINO",
    )

    private fun labelRenderer(sign: FacadeSign) = FacadeLabelRenderer(
        signs = listOf(sign),
        materialSampler = MaterialSampler(TestContent.loaded.palette),
        grid = TestContent.loaded.world.grid,
    )

    private fun pose(
        yawRadians: Double,
        position: WorldPosition = WorldPosition(5.0, -4.0),
    ) = ViewPose(
        position = position,
        eyeHeight = 0.6,
        yawRadians = yawRadians,
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

    private fun glyphRows(frame: AsciiFrame): List<String> = (0 until frame.height).map { row ->
        buildString {
            for (column in 0 until frame.width) append(frame.glyphAt(column, row).toChar())
        }
    }
}
