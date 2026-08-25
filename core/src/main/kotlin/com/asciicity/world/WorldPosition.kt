package com.asciicity.world

import kotlin.math.floor

data class WorldPosition(
    val x: Double,
    val z: Double,
) {
    init {
        require(x.isFinite()) { "World x position must be finite" }
        require(z.isFinite()) { "World z position must be finite" }
    }

    fun toGridCell(): GridCell = GridCell(
        x = floor(x).toInt(),
        z = floor(z).toInt(),
    )
}
