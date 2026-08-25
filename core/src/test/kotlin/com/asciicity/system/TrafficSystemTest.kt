package com.asciicity.system

import com.asciicity.TestContent
import com.asciicity.render.AsciiFrame
import com.asciicity.render.FontMetrics
import com.asciicity.render.ProjectionMetrics
import com.asciicity.render.ViewPose
import com.asciicity.traffic.TrafficCar
import com.asciicity.world.WorldPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TrafficSystemTest {
    @Test
    fun `traffic renders authored cars in stable id order without a world entity`() {
        val system = TrafficSystem(
            listOf(
                car("car.zeta", "sprite.car.van", 8.0),
                car("car.alpha", "sprite.car.taxi", 5.0),
            ),
            TestContent.loaded.sprites,
        )
        val frame = AsciiFrame(96, 30)
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

        assertEquals(2, system.carCount)
        assertEquals("car.alpha", system.carIdAt(0))
        assertEquals("car.zeta", system.carIdAt(1))
        assertTrue(system.render(frame, pose, metrics, 0.0) > 0)
    }

    @Test
    fun `taxi wordmarks stay compact instead of scaling with the car sprite`() {
        val system = TrafficSystem(
            listOf(car("car.taxi", "sprite.car.taxi", 0.0, label = "TAXI")),
            TestContent.loaded.sprites,
        )
        val frame = AsciiFrame(96, 30)
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

        system.render(frame, pose, metrics, 0.0)

        assertTrue((0 until frame.height).any { row ->
            buildString {
                for (column in 0 until frame.width) append(frame.glyphAt(column, row).toChar())
            }.contains("TAXI")
        })
    }

    private fun car(id: String, spriteId: String, x: Double, label: String? = null) = TrafficCar(
        id = id,
        spriteId = spriteId,
        speedCellsPerSecond = 1.0,
        phaseOffsetSeconds = 0.0,
        waypoints = listOf(WorldPosition(x, 6.0), WorldPosition(x, 8.0)),
        label = label,
    )
}
