package com.asciicity.world

enum class WorldDirection(
    val deltaX: Int,
    val deltaZ: Int,
) {
    NORTH(0, 1),
    EAST(1, 0),
    SOUTH(0, -1),
    WEST(-1, 0),
}
