package com.asciicity.content

import com.asciicity.world.GridCell
import com.asciicity.world.WorldDirection
import com.asciicity.world.WorldGrid

object WorldReachability {
    fun reachableCells(grid: WorldGrid, start: GridCell): Set<GridCell> {
        if (!grid.contains(start) || grid.cellAt(start).solid) {
            return emptySet()
        }
        val visited = linkedSetOf<GridCell>()
        val pending = ArrayDeque<GridCell>()
        visited += start
        pending += start

        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            for (direction in WorldDirection.entries) {
                val neighbor = current.moved(direction)
                if (grid.contains(neighbor) && !grid.cellAt(neighbor).solid && visited.add(neighbor)) {
                    pending += neighbor
                }
            }
        }
        return visited
    }
}
