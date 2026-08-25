package com.asciicity.render

import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GridRaycasterTest {
    @Test
    fun `cardinal diagonal corner outside and step limits are stable`() {
        val buffer = RayHitBuffer(16)
        GridRaycaster.cast(1.5, 1.5, 1.0, 0.0, 5, 5, 20.0, 16, buffer)
        assertEquals(RayHit(2, 1, RayHitSide.X_FACE, 0.5, 0.5), buffer.hitAt(0))
        assertEquals(RayHit(3, 1, RayHitSide.X_FACE, 1.5, 0.5), buffer.hitAt(1))

        GridRaycaster.cast(1.5, 1.5, 1.0, 1.0, 5, 5, 20.0, 16, buffer)
        assertEquals(RayHit(2, 2, RayHitSide.CORNER, 0.5, 0.0), buffer.hitAt(0))
        assertEquals(RayHit(3, 3, RayHitSide.CORNER, 1.5, 0.0), buffer.hitAt(1))

        assertEquals(0, GridRaycaster.cast(-0.1, 1.5, 1.0, 0.0, 5, 5, 20.0, 16, buffer))
        assertEquals(0, GridRaycaster.cast(0.5, 0.5, 1.0, 0.0, 1, 1, 20.0, 16, buffer))
        assertEquals(1, GridRaycaster.cast(1.5, 1.5, 1.0, 0.0, 5, 5, 20.0, 1, buffer))
    }

    @Test
    fun `fixed random rays are finite monotonic unique and bounded`() {
        val random = Random(0xA5C11)
        val buffer = RayHitBuffer(64)
        repeat(1_000) { fixture ->
            val originX = random.nextDouble(0.01, 15.99)
            val originZ = random.nextDouble(0.01, 15.99)
            val angle = random.nextDouble(0.0, Math.PI * 2.0)
            GridRaycaster.cast(
                originX,
                originZ,
                sin(angle),
                cos(angle),
                16,
                16,
                32.0,
                64,
                buffer,
            )
            var previousDistance = -1.0
            val cells = mutableSetOf<Pair<Int, Int>>()
            for (index in 0 until buffer.size) {
                val hit = buffer.hitAt(index)
                assertTrue(hit.distance.isFinite() && hit.distance >= previousDistance, "fixture=$fixture hit=$hit")
                assertTrue(hit.cellX in 0 until 16 && hit.cellZ in 0 until 16, "fixture=$fixture hit=$hit")
                assertTrue(cells.add(hit.cellX to hit.cellZ), "fixture=$fixture repeated cell $hit")
                assertTrue(hit.faceCoordinate in 0.0..<1.0, "fixture=$fixture hit=$hit")
                previousDistance = hit.distance
            }
        }
    }
}
