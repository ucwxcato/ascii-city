package com.asciicity.render

import com.asciicity.TestContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RainRendererTest {
    private val palette = TestContent.loaded.palette

    @Test
    fun `rain is deterministic for a time and advances over time`() {
        val renderer = RainRenderer(palette)
        val first = skyFrame()
        val repeated = skyFrame()
        val advanced = skyFrame()

        renderer.render(first, 2.0)
        renderer.render(repeated, 2.0)
        renderer.render(advanced, 3.0)

        assertTrue(frameGlyphs(first).contentEquals(frameGlyphs(repeated)))
        assertFalse(frameGlyphs(first).contentEquals(frameGlyphs(advanced)))
    }

    @Test
    fun `rain falls toward smaller frame rows`() {
        val renderer = RainRenderer(palette)
        val earlier = skyFrame()
        val later = skyFrame()

        renderer.render(earlier, 2.0)
        renderer.render(later, 2.2)

        assertTrue(
            (0 until earlier.width).any { column ->
                val earlierHead = lowestRainRow(earlier, column)
                val laterHead = lowestRainRow(later, column)
                earlierHead != null && laterHead != null && laterHead < earlierHead
            },
        )
    }

    @Test
    fun `rain does not draw over depth-tested city geometry`() {
        val frame = skyFrame()
        frame.set(10, 10, '#'.code, palette[PaletteKeys.WALL_FRONT], palette[PaletteKeys.SKY], 4.0f)

        RainRenderer(palette).render(frame, 2.0)

        assertEquals('#'.code, frame.glyphAt(10, 10))
        assertEquals(4.0f, frame.depthAt(10, 10))
    }

    private fun skyFrame() = AsciiFrame(96, 30).also {
        it.clear(' '.code, palette[PaletteKeys.FOG], palette[PaletteKeys.SKY])
    }

    private fun frameGlyphs(frame: AsciiFrame): IntArray = IntArray(frame.cellCount) { index ->
        frame.glyphAt(index % frame.width, index / frame.width)
    }

    private fun lowestRainRow(frame: AsciiFrame, column: Int): Int? =
        (0 until frame.height).firstOrNull { row -> frame.foregroundAt(column, row) == palette[PaletteKeys.RAIN] }
}
