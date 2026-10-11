package com.asciicity.content

import com.asciicity.world.WorldGrid

class MapLoader {
    fun parse(
        text: String,
        source: String,
        cells: CellRegistry,
        terrainHeights: ((width: Int, depth: Int) -> DoubleArray)? = null,
    ): WorldGrid {
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
        val lines = normalized.split('\n').toMutableList()
        if (lines.lastOrNull().isNullOrEmpty()) {
            lines.removeLastOrNull()
        }
        if (lines.isEmpty() || lines.all(String::isEmpty)) {
            throw ContentValidationException("$source:1:1: map must contain at least one non-empty row")
        }
        val width = lines.first().length
        if (width == 0) {
            throw ContentValidationException("$source:1:1: map rows must not be empty")
        }
        val depth = lines.size
        val resolved = IntArray(Math.multiplyExact(width, depth))

        for ((fileRow, line) in lines.withIndex()) {
            val lineNumber = fileRow + 1
            val tabIndex = line.indexOf('\t')
            if (tabIndex >= 0) {
                throw ContentValidationException("$source:$lineNumber:${tabIndex + 1}: tabs are not allowed")
            }
            if (line.length != width) {
                throw ContentValidationException(
                    "$source:$lineNumber:${line.length + 1}: ragged row has width ${line.length}; expected $width",
                )
            }
            val worldZ = depth - 1 - fileRow
            for ((x, token) in line.withIndex()) {
                if (token.code !in 32..126) {
                    throw ContentValidationException("$source:$lineNumber:${x + 1}: non-printable map token")
                }
                if (!cells.containsToken(token)) {
                    throw ContentValidationException("$source:$lineNumber:${x + 1}: unknown map token '$token'")
                }
                resolved[worldZ * width + x] = cells.indexForToken(token)
            }
        }
        val terrainVertices = terrainHeights?.invoke(width, depth)
            ?: DoubleArray(Math.multiplyExact(width + 1, depth + 1))
        if (terrainVertices.size != Math.multiplyExact(width + 1, depth + 1)) {
            throw ContentValidationException(
                "$source: terrain heightfield requires ${(width + 1) * (depth + 1)} corner heights, " +
                    "but received ${terrainVertices.size}",
            )
        }
        return WorldGrid.fromResolvedIndices(width, depth, cells, resolved, terrainVertices)
    }
}
