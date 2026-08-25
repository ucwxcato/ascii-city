package com.asciicity.render

import com.asciicity.world.WorldPosition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProjectionMetricsTest {
    @Test
    fun `center ray follows view and symmetric columns mirror`() {
        val metrics = metrics(128, 40, pitch = 0.0)
        assertEquals(0.0, metrics.cameraPlaneX(64.0), 1e-12)
        assertEquals(64.0, metrics.projectCameraPlaneX(0.0), 1e-12)
        assertEquals(17.25, metrics.projectCameraPlaneX(metrics.cameraPlaneX(17.25)), 1e-12)
        assertEquals(
            -metrics.cameraPlaneXForColumn(0),
            metrics.cameraPlaneXForColumn(127),
            1e-12,
        )
        val pose = pose()
        val leftX = metrics.rayDirectionX(pose, 0)
        val rightX = metrics.rayDirectionX(pose, 127)
        assertEquals(-leftX, rightX, 1e-12)
        assertTrue(metrics.rayDirectionZ(pose, 64) > 0.99)
    }

    @Test
    fun `pitch moves horizon and distance scales projected height`() {
        val level = metrics(128, 40, pitch = 0.0)
        val lookingUp = metrics(128, 40, pitch = Math.toRadians(20.0))
        val lookingDown = metrics(128, 40, pitch = Math.toRadians(-20.0))
        assertTrue(lookingUp.horizonRow < level.horizonRow)
        assertTrue(lookingDown.horizonRow > level.horizonRow)

        val nearHeight = level.projectWorldHeight(2.0, 0.0, 2.0) - level.horizonRow
        val farHeight = level.projectWorldHeight(2.0, 0.0, 4.0) - level.horizonRow
        assertEquals(nearHeight, farHeight * 2.0, 1e-12)
    }

    @Test
    fun `ground and ceiling planes cast on opposite sides of the horizon`() {
        val metrics = metrics(128, 40, pitch = 0.0)
        val eyeHeight = 0.6
        val ceilingHeight = 1.2

        assertEquals(
            metrics.groundDepthAtRow(19, eyeHeight),
            metrics.ceilingDepthAtRow(20, eyeHeight, ceilingHeight),
            1e-12,
        )
        assertEquals(Double.POSITIVE_INFINITY, metrics.groundDepthAtRow(20, eyeHeight))
        assertEquals(Double.POSITIVE_INFINITY, metrics.ceilingDepthAtRow(19, eyeHeight, ceilingHeight))
    }

    @Test
    fun `same physical aspect preserves normalized projection across resolutions`() {
        val hd = metrics(128, 40, pitch = 0.0)
        val minimum = metrics(96, 30, pitch = 0.0)
        assertEquals(hd.verticalFovRadians, minimum.verticalFovRadians, 1e-12)
        assertEquals(hd.focalRows / 40.0, minimum.focalRows / 30.0, 1e-12)
    }

    @Test
    fun `ultrawide projection reveals more horizontally while preserving vertical framing`() {
        val reference = metrics(128, 40, pitch = 0.0)
        val ultrawide = metrics(172, 40, pitch = 0.0)

        assertTrue(ultrawide.effectiveHorizontalFovRadians > reference.effectiveHorizontalFovRadians)
        assertEquals(reference.verticalFovRadians, ultrawide.verticalFovRadians, 1e-12)
        assertEquals(reference.focalRows, ultrawide.focalRows, 1e-12)
        assertTrue(ultrawide.cameraPlaneXForColumn(0) < reference.cameraPlaneXForColumn(0))
        assertTrue(ultrawide.cameraPlaneXForColumn(171) > reference.cameraPlaneXForColumn(127))
    }

    @Test
    fun `extreme aspect ratios remain inside supported effective fov bounds`() {
        val extremeWide = metrics(512, 30, pitch = 0.0)
        val extremeTall = metrics(96, 160, pitch = 0.0)

        assertEquals(ViewPose.MAX_FOV_RADIANS, extremeWide.effectiveHorizontalFovRadians, 1e-12)
        assertEquals(ViewPose.MIN_FOV_RADIANS, extremeTall.effectiveHorizontalFovRadians, 1e-12)
    }

    private fun metrics(columns: Int, rows: Int, pitch: Double) = ProjectionMetrics(
        columns = columns,
        rows = rows,
        pixelWidth = columns * FontMetrics.CELL_WIDTH,
        pixelHeight = rows * FontMetrics.CELL_HEIGHT,
        horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
        pitchRadians = pitch,
    )

    private fun pose() = ViewPose(
        position = WorldPosition(0.0, 0.0),
        eyeHeight = 0.6,
        yawRadians = 0.0,
        pitchRadians = 0.0,
        horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
    )
}
