package com.asciicity.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParkFixtureSilhouetteTest {
    @Test
    fun `bench has slats and legs with open space underneath`() {
        val tags = setOf("park", "bench", "fixture")

        assertEquals(ParkFixtureSilhouette.Part.BENCH_SLAT, ParkFixtureSilhouette.partAt(tags, 0.5, 0.7, 1.0))
        assertEquals(ParkFixtureSilhouette.Part.BENCH_LEG, ParkFixtureSilhouette.partAt(tags, 0.2, 0.2, 1.0))
        assertNull(ParkFixtureSilhouette.partAt(tags, 0.5, 0.2, 1.0))
    }

    @Test
    fun `bin is narrower than its cell`() {
        val tags = setOf("park", "bin", "fixture")

        assertEquals(ParkFixtureSilhouette.Part.BIN, ParkFixtureSilhouette.partAt(tags, 0.5, 0.5, 1.0))
        assertNull(ParkFixtureSilhouette.partAt(tags, 0.1, 0.5, 1.0))
    }

    @Test
    fun `fountain separates stone base from water crown`() {
        val tags = setOf("park", "fountain", "fixture", "water")

        assertEquals(ParkFixtureSilhouette.Part.FOUNTAIN_STONE, ParkFixtureSilhouette.partAt(tags, 0.5, 0.6, 2.0))
        assertEquals(ParkFixtureSilhouette.Part.FOUNTAIN_WATER, ParkFixtureSilhouette.partAt(tags, 0.5, 1.7, 2.0))
        assertNull(ParkFixtureSilhouette.partAt(tags, 0.2, 1.7, 2.0))
    }

    @Test
    fun `pavilion has open sides beneath its roof`() {
        val tags = setOf("park", "pavilion", "fixture")

        assertEquals(ParkFixtureSilhouette.Part.PAVILION_POST, ParkFixtureSilhouette.partAt(tags, 0.08, 1.5, 4.0))
        assertEquals(ParkFixtureSilhouette.Part.PAVILION_ROOF, ParkFixtureSilhouette.partAt(tags, 0.5, 3.5, 4.0))
        assertNull(ParkFixtureSilhouette.partAt(tags, 0.5, 1.5, 4.0))
    }

    @Test
    fun `tent has triangular canvas and an open entrance`() {
        val tags = setOf("park", "nature", "tent", "fixture")

        assertEquals(ParkFixtureSilhouette.Part.TENT_CANVAS, ParkFixtureSilhouette.partAt(tags, 0.2, 0.3, 2.0))
        assertEquals(ParkFixtureSilhouette.Part.TENT_RIDGE, ParkFixtureSilhouette.partAt(tags, 0.5, 1.9, 2.0))
        assertNull(ParkFixtureSilhouette.partAt(tags, 0.5, 0.3, 2.0))
        assertNull(ParkFixtureSilhouette.partAt(tags, 0.1, 1.5, 2.0))
    }

    @Test
    fun `waterfall separates animated flow from rock shoulders and spray`() {
        val tags = setOf("park", "nature", "water", "waterfall", "fixture")

        assertEquals(ParkFixtureSilhouette.Part.WATERFALL_ROCK, ParkFixtureSilhouette.partAt(tags, 0.05, 3.0, 7.0))
        assertEquals(ParkFixtureSilhouette.Part.WATERFALL_FLOW, ParkFixtureSilhouette.partAt(tags, 0.5, 3.0, 7.0))
        assertEquals(ParkFixtureSilhouette.Part.WATERFALL_SPRAY, ParkFixtureSilhouette.partAt(tags, 0.5, 0.3, 7.0))
    }

    @Test
    fun `slot machine has feet cabinet button reels marquee and top light`() {
        val tags = setOf("interior", "casino", "slot-machine", "fixture")

        assertEquals(ParkFixtureSilhouette.Part.SLOT_CABINET, ParkFixtureSilhouette.partAt(tags, 0.25, 0.1, 2.0))
        assertNull(ParkFixtureSilhouette.partAt(tags, 0.5, 0.1, 2.0))
        assertEquals(ParkFixtureSilhouette.Part.SLOT_CABINET, ParkFixtureSilhouette.partAt(tags, 0.5, 0.5, 2.0))
        assertEquals(ParkFixtureSilhouette.Part.SLOT_BUTTON, ParkFixtureSilhouette.partAt(tags, 0.68, 0.7, 2.0))
        assertEquals(ParkFixtureSilhouette.Part.SLOT_FRAME, ParkFixtureSilhouette.partAt(tags, 0.15, 1.2, 2.0))
        assertEquals(ParkFixtureSilhouette.Part.SLOT_REEL, ParkFixtureSilhouette.partAt(tags, 0.5, 1.2, 2.0))
        assertEquals(ParkFixtureSilhouette.Part.SLOT_MARQUEE, ParkFixtureSilhouette.partAt(tags, 0.5, 1.7, 2.0))
        assertEquals(ParkFixtureSilhouette.Part.SLOT_TOP_LIGHT, ParkFixtureSilhouette.partAt(tags, 0.5, 1.95, 2.0))
        assertNull(ParkFixtureSilhouette.partAt(tags, 0.02, 1.2, 2.0))
    }
}
