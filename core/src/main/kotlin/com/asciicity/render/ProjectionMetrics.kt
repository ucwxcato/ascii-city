package com.asciicity.render

import kotlin.math.atan
import kotlin.math.tan

data class ProjectionMetrics(
    val columns: Int,
    val rows: Int,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val horizontalFovRadians: Double,
    val pitchRadians: Double,
) {
    init {
        require(columns > 0 && rows > 0) { "Projection grid dimensions must be positive" }
        require(pixelWidth > 0 && pixelHeight > 0) { "Projection pixel dimensions must be positive" }
        require(horizontalFovRadians in ViewPose.MIN_FOV_RADIANS..ViewPose.MAX_FOV_RADIANS) {
            "Projection FOV is outside the supported range"
        }
        require(pitchRadians.isFinite() && pitchRadians in -ViewPose.MAX_PITCH_RADIANS..ViewPose.MAX_PITCH_RADIANS) {
            "Projection pitch is outside the supported range"
        }
    }

    val physicalAspect: Double = pixelWidth.toDouble() / pixelHeight.toDouble()
    val effectiveHorizontalFovRadians: Double = (
        2.0 * atan(tan(horizontalFovRadians * 0.5) * physicalAspect / REFERENCE_ASPECT)
        ).coerceIn(ViewPose.MIN_FOV_RADIANS, ViewPose.MAX_FOV_RADIANS)
    val halfFovTangent: Double = tan(effectiveHorizontalFovRadians * 0.5)
    val verticalFovRadians: Double = 2.0 * atan(halfFovTangent / physicalAspect)
    val focalRows: Double = rows * 0.5 / tan(verticalFovRadians * 0.5)
    val horizonRow: Double = rows * 0.5 - tan(pitchRadians) * focalRows

    fun cameraPlaneXForColumn(column: Int): Double {
        require(column in 0 until columns) { "Projection column $column is outside 0 until $columns" }
        return cameraPlaneX(column + 0.5)
    }

    fun cameraPlaneX(screenX: Double): Double {
        require(screenX.isFinite() && screenX in 0.0..columns.toDouble()) {
            "Screen x must be finite and inside the projection"
        }
        return (2.0 * screenX / columns - 1.0) * halfFovTangent
    }

    fun projectCameraPlaneX(cameraPlaneX: Double): Double {
        require(cameraPlaneX.isFinite()) { "Camera-plane x must be finite" }
        return (cameraPlaneX / halfFovTangent + 1.0) * columns * 0.5
    }

    fun rayDirectionX(pose: ViewPose, column: Int): Double =
        pose.forwardX + pose.rightX * cameraPlaneXForColumn(column)

    fun rayDirectionZ(pose: ViewPose, column: Int): Double =
        pose.forwardZ + pose.rightZ * cameraPlaneXForColumn(column)

    fun projectWorldHeight(worldHeight: Double, eyeHeight: Double, perpendicularDepth: Double): Double {
        require(worldHeight.isFinite() && eyeHeight.isFinite()) { "Projected heights must be finite" }
        require(perpendicularDepth > 0.0 && perpendicularDepth.isFinite()) {
            "Projection depth must be positive and finite"
        }
        return horizonRow + (worldHeight - eyeHeight) * focalRows / perpendicularDepth
    }

    fun groundDepthAtRow(row: Int, eyeHeight: Double): Double {
        require(row in 0 until rows) { "Projection row $row is outside 0 until $rows" }
        require(eyeHeight > 0.0 && eyeHeight.isFinite()) { "Eye height must be positive and finite" }
        val distanceBelowHorizon = horizonRow - (row + 0.5)
        return if (distanceBelowHorizon <= 0.0) Double.POSITIVE_INFINITY
        else eyeHeight * focalRows / distanceBelowHorizon
    }

    fun ceilingDepthAtRow(row: Int, eyeHeight: Double, ceilingHeight: Double): Double {
        require(row in 0 until rows) { "Projection row $row is outside 0 until $rows" }
        require(eyeHeight > 0.0 && eyeHeight.isFinite()) { "Eye height must be positive and finite" }
        require(ceilingHeight > eyeHeight && ceilingHeight.isFinite()) {
            "Ceiling height must be finite and greater than eye height"
        }
        val distanceAboveHorizon = row + 0.5 - horizonRow
        return if (distanceAboveHorizon <= 0.0) Double.POSITIVE_INFINITY
        else (ceilingHeight - eyeHeight) * focalRows / distanceAboveHorizon
    }

    companion object {
        const val REFERENCE_ASPECT: Double = 16.0 / 9.0
    }
}
