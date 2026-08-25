package com.asciicity.world

data class GridCell(
    val x: Int,
    val z: Int,
) {
    fun moved(direction: WorldDirection): GridCell = GridCell(
        x = x + direction.deltaX,
        z = z + direction.deltaZ,
    )
}
