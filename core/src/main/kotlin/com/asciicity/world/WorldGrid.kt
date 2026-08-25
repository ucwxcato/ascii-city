package com.asciicity.world

import com.asciicity.content.CellRegistry

class WorldGrid private constructor(
    val width: Int,
    val depth: Int,
    private val registry: CellRegistry,
    cellIndices: IntArray,
) {
    private val cellIndices = cellIndices.copyOf()

    init {
        require(width > 0) { "World grid width must be positive" }
        require(depth > 0) { "World grid depth must be positive" }
        require(this.cellIndices.size == Math.multiplyExact(width, depth)) {
            "World grid requires ${width * depth} cells, but received ${this.cellIndices.size}"
        }
        for (cellIndex in this.cellIndices) {
            registry.definitionAt(cellIndex)
        }
    }

    fun contains(cell: GridCell): Boolean = cell.x in 0 until width && cell.z in 0 until depth

    fun contains(x: Int, z: Int): Boolean = x in 0 until width && z in 0 until depth

    fun cellIndexAt(cell: GridCell): Int = cellIndices[arrayIndex(cell)]

    fun cellAt(cell: GridCell): CellDefinition = registry.definitionAt(cellIndexAt(cell))

    fun cellAt(x: Int, z: Int): CellDefinition = registry.definitionAt(cellIndices[arrayIndex(x, z)])

    fun setCell(cell: GridCell, definitionId: String) {
        cellIndices[arrayIndex(cell)] = registry.indexForId(definitionId)
    }

    fun forEachCell(action: (GridCell, CellDefinition) -> Unit) {
        for (z in 0 until depth) {
            for (x in 0 until width) {
                val cell = GridCell(x, z)
                action(cell, cellAt(cell))
            }
        }
    }

    private fun arrayIndex(cell: GridCell): Int {
        return arrayIndex(cell.x, cell.z)
    }

    private fun arrayIndex(x: Int, z: Int): Int {
        require(contains(x, z)) { "World cell ($x,$z) is outside width=$width depth=$depth" }
        return z * width + x
    }

    companion object {
        fun fromResolvedIndices(
            width: Int,
            depth: Int,
            registry: CellRegistry,
            cellIndices: IntArray,
        ): WorldGrid = WorldGrid(width, depth, registry, cellIndices)
    }
}
