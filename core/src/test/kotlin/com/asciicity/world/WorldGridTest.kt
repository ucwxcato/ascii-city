package com.asciicity.world

import com.asciicity.TestContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorldGridTest {
    private val cells = TestContent.loaded.cells

    @Test
    fun `row major storage maps every corner and iteration order`() {
        val indices = intArrayOf(
            cells.indexForId("road"),
            cells.indexForId("sidewalk"),
            cells.indexForId("alley"),
            cells.indexForId("plaza"),
            cells.indexForId("building.low"),
            cells.indexForId("building.tall"),
        )
        val grid = WorldGrid.fromResolvedIndices(3, 2, cells, indices)

        assertEquals("road", grid.cellAt(GridCell(0, 0)).id)
        assertEquals("alley", grid.cellAt(GridCell(2, 0)).id)
        assertEquals("plaza", grid.cellAt(GridCell(0, 1)).id)
        assertEquals("building.tall", grid.cellAt(GridCell(2, 1)).id)
        val visited = mutableListOf<GridCell>()
        grid.forEachCell { cell, _ -> visited += cell }
        assertEquals(
            listOf(GridCell(0, 0), GridCell(1, 0), GridCell(2, 0), GridCell(0, 1), GridCell(1, 1), GridCell(2, 1)),
            visited,
        )
    }

    @Test
    fun `bounds mutation and private-copy behavior are enforced`() {
        val source = IntArray(4) { cells.indexForId("road") }
        val grid = WorldGrid.fromResolvedIndices(2, 2, cells, source)
        source[0] = cells.indexForId("building.tall")
        assertEquals("road", grid.cellAt(GridCell(0, 0)).id)

        grid.setCell(GridCell(1, 1), "building.low")
        assertEquals("building.low", grid.cellAt(GridCell(1, 1)).id)
        assertTrue(grid.contains(GridCell(0, 0)))
        assertFalse(grid.contains(GridCell(-1, 0)))
        assertFalse(grid.contains(GridCell(2, 1)))
        assertFailsWith<IllegalArgumentException> { grid.cellAt(GridCell(2, 0)) }
        assertFailsWith<IllegalArgumentException> {
            WorldGrid.fromResolvedIndices(2, 2, cells, IntArray(3))
        }
    }
}
