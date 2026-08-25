package com.asciicity.content

import com.asciicity.TestContent
import com.asciicity.world.GridCell
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MapLoaderTest {
    private val cells = TestContent.loaded.cells
    private val loader = MapLoader()

    @Test
    fun `empty ragged tabbed and unknown-token maps report exact locations`() {
        assertEquals(
            "empty.txt:1:1: map must contain at least one non-empty row",
            failure("").message,
        )
        assertEquals(
            "ragged.txt:2:3: ragged row has width 2; expected 3",
            failure("...\n..", "ragged.txt").message,
        )
        assertEquals(
            "tabbed.txt:1:2: tabs are not allowed",
            failure(".\t.", "tabbed.txt").message,
        )
        assertEquals(
            "unknown.txt:1:2: unknown map token '?'",
            failure(".?", "unknown.txt").message,
        )
    }

    @Test
    fun `lf and crlf maps resolve identically and north row flips to high z`() {
        val lf = loader.parse(".l\nm,\n", "lf.txt", cells)
        val crlf = loader.parse(".l\r\nm,\r\n", "crlf.txt", cells)

        for (z in 0 until 2) {
            for (x in 0 until 2) {
                assertEquals(lf.cellAt(GridCell(x, z)).id, crlf.cellAt(GridCell(x, z)).id)
            }
        }
        assertEquals("road", lf.cellAt(GridCell(0, 1)).id)
        assertEquals("building.low", lf.cellAt(GridCell(1, 1)).id)
        assertEquals("building.mid", lf.cellAt(GridCell(0, 0)).id)
        assertEquals("sidewalk", lf.cellAt(GridCell(1, 0)).id)
    }

    private fun failure(text: String, source: String = "empty.txt"): ContentValidationException =
        assertFailsWith { loader.parse(text, source, cells) }
}
