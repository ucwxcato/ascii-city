package com.asciicity.render

import kotlin.math.abs
import kotlin.math.floor

object GridRaycaster {
    private const val CORNER_EPSILON = 1e-10

    fun cast(
        originX: Double,
        originZ: Double,
        directionX: Double,
        directionZ: Double,
        gridWidth: Int,
        gridDepth: Int,
        maxDistance: Double,
        maxSteps: Int,
        output: RayHitBuffer,
    ): Int {
        output.size = 0
        return traverse(
            originX = originX,
            originZ = originZ,
            directionX = directionX,
            directionZ = directionZ,
            gridWidth = gridWidth,
            gridDepth = gridDepth,
            maxDistance = maxDistance,
            maxSteps = minOf(maxSteps, output.capacity),
            visitor = RayVisitor { cellX, cellZ, side, distance, faceCoordinate ->
                val index = output.size
                output.cellXs[index] = cellX
                output.cellZs[index] = cellZ
                output.sides[index] = side.ordinal.toByte()
                output.distances[index] = distance
                output.faceCoordinates[index] = faceCoordinate
                output.size += 1
                true
            },
        )
    }

    fun traverse(
        originX: Double,
        originZ: Double,
        directionX: Double,
        directionZ: Double,
        gridWidth: Int,
        gridDepth: Int,
        maxDistance: Double,
        maxSteps: Int,
        visitor: RayVisitor,
    ): Int {
        require(originX.isFinite() && originZ.isFinite()) { "Ray origin must be finite" }
        require(directionX.isFinite() && directionZ.isFinite()) { "Ray direction must be finite" }
        require(directionX != 0.0 || directionZ != 0.0) { "Ray direction must not be zero" }
        require(gridWidth > 0 && gridDepth > 0) { "Raycast grid dimensions must be positive" }
        require(maxDistance > 0.0 && maxDistance.isFinite()) { "Maximum ray distance must be positive and finite" }
        require(maxSteps > 0) { "Maximum ray steps must be positive" }

        var cellX = floor(originX).toInt()
        var cellZ = floor(originZ).toInt()
        if (cellX !in 0 until gridWidth || cellZ !in 0 until gridDepth) return 0

        val stepX = directionX.compareTo(0.0)
        val stepZ = directionZ.compareTo(0.0)
        val deltaX = if (stepX == 0) Double.POSITIVE_INFINITY else abs(1.0 / directionX)
        val deltaZ = if (stepZ == 0) Double.POSITIVE_INFINITY else abs(1.0 / directionZ)
        var sideDistanceX = when {
            stepX > 0 -> (cellX + 1.0 - originX) * deltaX
            stepX < 0 -> (originX - cellX) * deltaX
            else -> Double.POSITIVE_INFINITY
        }
        var sideDistanceZ = when {
            stepZ > 0 -> (cellZ + 1.0 - originZ) * deltaZ
            stepZ < 0 -> (originZ - cellZ) * deltaZ
            else -> Double.POSITIVE_INFINITY
        }

        var visited = 0
        while (visited < maxSteps) {
            val side: RayHitSide
            val distance: Double
            if (abs(sideDistanceX - sideDistanceZ) <= CORNER_EPSILON) {
                distance = sideDistanceX
                cellX += stepX
                cellZ += stepZ
                sideDistanceX += deltaX
                sideDistanceZ += deltaZ
                side = RayHitSide.CORNER
            } else if (sideDistanceX < sideDistanceZ) {
                distance = sideDistanceX
                cellX += stepX
                sideDistanceX += deltaX
                side = RayHitSide.X_FACE
            } else {
                distance = sideDistanceZ
                cellZ += stepZ
                sideDistanceZ += deltaZ
                side = RayHitSide.Z_FACE
            }

            if (distance > maxDistance || !distance.isFinite()) break
            if (cellX !in 0 until gridWidth || cellZ !in 0 until gridDepth) break

            val faceCoordinate = when (side) {
                RayHitSide.X_FACE -> fractional(originZ + directionZ * distance)
                RayHitSide.Z_FACE -> fractional(originX + directionX * distance)
                RayHitSide.CORNER -> 0.0
            }
            visited += 1
            if (!visitor.visit(cellX, cellZ, side, distance, faceCoordinate)) break
        }
        return visited
    }

    private fun fractional(value: Double): Double = value - floor(value)
}

fun interface RayVisitor {
    fun visit(
        cellX: Int,
        cellZ: Int,
        side: RayHitSide,
        distance: Double,
        faceCoordinate: Double,
    ): Boolean
}
