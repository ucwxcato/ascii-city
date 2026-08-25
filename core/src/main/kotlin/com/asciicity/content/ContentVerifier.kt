package com.asciicity.content

import com.asciicity.world.GridCell

object ContentVerifier {
    fun verify(content: LoadedContent): ContentReport {
        val cellCounts = sortedMapOf<String, Int>()
        val heightCounts = sortedMapOf<Int, Int>()
        val materialCounts = sortedMapOf<String, Int>()

        content.world.grid.forEachCell { _, definition ->
            cellCounts[definition.id] = cellCounts.getOrDefault(definition.id, 0) + 1
            heightCounts[definition.height] = heightCounts.getOrDefault(definition.height, 0) + 1
            materialCounts[definition.groundMaterialId] = materialCounts.getOrDefault(definition.groundMaterialId, 0) + 1
            materialCounts[definition.facadeMaterialId] = materialCounts.getOrDefault(definition.facadeMaterialId, 0) + 1
        }

        val entityCounts = content.world.allEntities()
            .groupingBy { it.type.name.lowercase() }
            .eachCount()
            .toSortedMap()

        val reachable = WorldReachability.reachableCells(
            content.world.grid,
            content.world.player.position.toGridCell(),
        )
        return ContentReport(
            mapId = content.world.mapId,
            mapVersion = content.world.mapVersion,
            width = content.world.grid.width,
            depth = content.world.grid.depth,
            cellCounts = cellCounts,
            heightCounts = heightCounts,
            materialCellCounts = materialCounts,
            entityTypeCounts = entityCounts,
            reachableCellCount = reachable.size,
            debugFootprint = debugFootprint(content),
        )
    }

    private fun debugFootprint(content: LoadedContent): String {
        val grid = content.world.grid
        val stepX = (grid.width / 48).coerceAtLeast(1)
        val stepZ = (grid.depth / 24).coerceAtLeast(1)
        return buildString {
            var z = grid.depth - 1
            while (z >= 0) {
                var x = 0
                while (x < grid.width) {
                    append(grid.cellAt(GridCell(x, z)).token)
                    x += stepX
                }
                append('\n')
                z -= stepZ
            }
        }.trimEnd()
    }
}
