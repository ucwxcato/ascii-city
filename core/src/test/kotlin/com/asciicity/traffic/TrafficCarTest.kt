package com.asciicity.traffic

import com.asciicity.world.WorldPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TrafficCarTest {
    private val loop = listOf(
        WorldPosition(0.0, 0.0),
        WorldPosition(10.0, 0.0),
        WorldPosition(10.0, 10.0),
        WorldPosition(0.0, 10.0),
    )

    @Test
    fun `cars move at a constant speed and wrap their implicit loop`() {
        val car = TrafficCar("car.test", "sprite.car.taxi", 1.0, 0.0, loop)

        assertEquals(WorldPosition(0.0, 0.0), car.positionAt(0.0))
        assertEquals(WorldPosition(5.0, 0.0), car.positionAt(5.0))
        assertEquals(WorldPosition(10.0, 5.0), car.positionAt(15.0))
        assertEquals(WorldPosition(5.0, 10.0), car.positionAt(25.0))
        assertEquals(WorldPosition(0.0, 5.0), car.positionAt(35.0))
        assertEquals(WorldPosition(5.0, 0.0), car.positionAt(45.0))
    }

    @Test
    fun `phase offsets spread cars across the same route without changing speed`() {
        val car = TrafficCar("car.test", "sprite.car.taxi", 2.0, 2.5, loop)

        assertEquals(WorldPosition(5.0, 0.0), car.positionAt(0.0))
        assertEquals(WorldPosition(7.0, 0.0), car.positionAt(1.0))
    }

    @Test
    fun `cars reject adjacent duplicate waypoints and nonpositive speeds`() {
        assertFailsWith<IllegalArgumentException> {
            TrafficCar(
                "car.test",
                "sprite.car.taxi",
                1.0,
                0.0,
                listOf(WorldPosition(0.0, 0.0), WorldPosition(0.0, 0.0)),
            )
        }
        assertFailsWith<IllegalArgumentException> {
            TrafficCar("car.test", "sprite.car.taxi", 0.0, 0.0, loop)
        }
    }
}
