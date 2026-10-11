package com.asciicity.world

import com.asciicity.content.CellRegistry

class WorldGrid private constructor(
    val width: Int,
    val depth: Int,
    private val registry: CellRegistry,
    cellIndices: IntArray,
    terrainVertexHeightsSouthToNorth: DoubleArray,
) {
    private val cellIndices = cellIndices.copyOf()
    private val terrainVertexHeights = terrainVertexHeightsSouthToNorth.copyOf()
    val hasTerrainElevation: Boolean = terrainVertexHeights.any { it != 0.0 }
    val minimumTerrainHeight: Double = terrainVertexHeights.minOrNull() ?: 0.0
    val maximumTerrainHeight: Double = terrainVertexHeights.maxOrNull() ?: 0.0

    init {
        require(width > 0) { "World grid width must be positive" }
        require(depth > 0) { "World grid depth must be positive" }
        require(this.cellIndices.size == Math.multiplyExact(width, depth)) {
            "World grid requires ${width * depth} cells, but received ${this.cellIndices.size}"
        }
        val expectedTerrainVertices = Math.multiplyExact(width + 1, depth + 1)
        require(this.terrainVertexHeights.size == expectedTerrainVertices) {
            "World grid requires $expectedTerrainVertices terrain heights, but received ${this.terrainVertexHeights.size}"
        }
        require(this.terrainVertexHeights.all(Double::isFinite)) {
            "World grid terrain heights must be finite"
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

    fun terrainHeightAt(worldX: Double, worldZ: Double): Double {
        require(worldX.isFinite() && worldX >= 0.0 && worldX <= width.toDouble()) {
            "Terrain x must be inside the world grid"
        }
        require(worldZ.isFinite() && worldZ >= 0.0 && worldZ <= depth.toDouble()) {
            "Terrain z must be inside the world grid"
        }
        val x = worldX.coerceIn(0.0, width.toDouble())
        val z = worldZ.coerceIn(0.0, depth.toDouble())
        val x0 = kotlin.math.floor(x).toInt().coerceAtMost(width - 1)
        val z0 = kotlin.math.floor(z).toInt().coerceAtMost(depth - 1)
        val tx = x - x0
        val tz = z - z0
        val stride = width + 1
        val h00 = terrainVertexHeights[z0 * stride + x0]
        val h10 = terrainVertexHeights[z0 * stride + x0 + 1]
        val h01 = terrainVertexHeights[(z0 + 1) * stride + x0]
        val h11 = terrainVertexHeights[(z0 + 1) * stride + x0 + 1]
        val south = h00 + (h10 - h00) * tx
        val north = h01 + (h11 - h01) * tx
        return south + (north - south) * tz
    }

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
            terrainVertexHeightsSouthToNorth: DoubleArray = DoubleArray(Math.multiplyExact(width + 1, depth + 1)),
        ): WorldGrid = WorldGrid(width, depth, registry, cellIndices, terrainVertexHeightsSouthToNorth)
    }
}
