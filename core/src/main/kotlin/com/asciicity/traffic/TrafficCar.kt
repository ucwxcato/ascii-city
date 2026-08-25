package com.asciicity.traffic

import com.asciicity.world.WorldPosition
import kotlin.math.hypot

/** A visual-only car that loops its authored road waypoints at a constant speed. */
data class TrafficCar(
    val id: String,
    val spriteId: String,
    val speedCellsPerSecond: Double,
    val phaseOffsetSeconds: Double,
    val waypoints: List<WorldPosition>,
    val label: String? = null,
) {
    private val route = waypoints.toList()
    private val segmentLengths = DoubleArray(route.size) { index ->
        val start = route[index]
        val end = route[(index + 1) % route.size]
        hypot(end.x - start.x, end.z - start.z)
    }
    private val loopLength = segmentLengths.sum()

    init {
        require(id.isNotBlank()) { "Traffic car ID must not be blank" }
        require(spriteId.isNotBlank()) { "Traffic car '$id' sprite ID must not be blank" }
        require(speedCellsPerSecond in MINIMUM_SPEED..MAXIMUM_SPEED && speedCellsPerSecond.isFinite()) {
            "Traffic car '$id' speed must be between $MINIMUM_SPEED and $MAXIMUM_SPEED"
        }
        require(phaseOffsetSeconds >= 0.0 && phaseOffsetSeconds.isFinite()) {
            "Traffic car '$id' phase offset must be finite and non-negative"
        }
        require(route.size >= MINIMUM_WAYPOINTS) {
            "Traffic car '$id' must have at least $MINIMUM_WAYPOINTS waypoints"
        }
        require(label == null || label.isNotBlank() && label.length <= MAXIMUM_LABEL_LENGTH && label.all { it.code in 32..126 }) {
            "Traffic car '$id' label must be non-blank printable ASCII and at most $MAXIMUM_LABEL_LENGTH glyphs"
        }
        require(segmentLengths.all { it > 0.0 && it.isFinite() }) {
            "Traffic car '$id' must not repeat adjacent waypoints"
        }
    }

    fun positionAt(timeSeconds: Double): WorldPosition {
        require(timeSeconds >= 0.0 && timeSeconds.isFinite()) { "Traffic time must be finite and non-negative" }
        var distance = ((timeSeconds + phaseOffsetSeconds) * speedCellsPerSecond) % loopLength
        for (index in route.indices) {
            val segmentLength = segmentLengths[index]
            if (distance <= segmentLength) {
                val start = route[index]
                val end = route[(index + 1) % route.size]
                val progress = distance / segmentLength
                return WorldPosition(
                    x = start.x + (end.x - start.x) * progress,
                    z = start.z + (end.z - start.z) * progress,
                )
            }
            distance -= segmentLength
        }
        return route.first()
    }

    companion object {
        const val MINIMUM_WAYPOINTS = 2
        const val MINIMUM_SPEED = 0.1
        const val MAXIMUM_SPEED = 4.0
        const val MAXIMUM_LABEL_LENGTH = 12
    }
}
