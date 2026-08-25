package com.asciicity.render

enum class RayHitSide {
    X_FACE,
    Z_FACE,
    CORNER,
}

data class RayHit(
    val cellX: Int,
    val cellZ: Int,
    val side: RayHitSide,
    val distance: Double,
    val faceCoordinate: Double,
)

class RayHitBuffer(
    val capacity: Int,
) {
    internal val cellXs = IntArray(capacity)
    internal val cellZs = IntArray(capacity)
    internal val sides = ByteArray(capacity)
    internal val distances = DoubleArray(capacity)
    internal val faceCoordinates = DoubleArray(capacity)

    var size: Int = 0
        internal set

    init {
        require(capacity > 0) { "Ray hit capacity must be positive" }
    }

    fun hitAt(index: Int): RayHit {
        require(index in 0 until size) { "Ray hit index $index is outside 0 until $size" }
        return RayHit(
            cellX = cellXs[index],
            cellZ = cellZs[index],
            side = RayHitSide.entries[sides[index].toInt()],
            distance = distances[index],
            faceCoordinate = faceCoordinates[index],
        )
    }
}
