package com.asciicity.world

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WorldValueTypesTest {
    @Test
    fun `positions floor into grid cells including negative coordinates`() {
        assertEquals(GridCell(4, 7), WorldPosition(4.99, 7.01).toGridCell())
        assertEquals(GridCell(-1, -2), WorldPosition(-0.01, -1.01).toGridCell())
        assertEquals(WorldPosition(4.99, 7.01), WorldPosition(4.99, 7.01))
    }

    @Test
    fun `cardinal direction vectors and grid movement agree`() {
        val origin = GridCell(10, 20)
        assertEquals(GridCell(10, 21), origin.moved(WorldDirection.NORTH))
        assertEquals(GridCell(11, 20), origin.moved(WorldDirection.EAST))
        assertEquals(GridCell(10, 19), origin.moved(WorldDirection.SOUTH))
        assertEquals(GridCell(9, 20), origin.moved(WorldDirection.WEST))
        assertEquals(setOf(0 to 1, 1 to 0, 0 to -1, -1 to 0), WorldDirection.entries.map { it.deltaX to it.deltaZ }.toSet())
    }

    @Test
    fun `entity IDs are stable value objects with a strict format`() {
        assertEquals(EntityId("npc.rin"), EntityId("npc.rin"))
        assertFailsWith<IllegalArgumentException> { EntityId("NPC Rin") }
    }
}
