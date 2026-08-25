package com.asciicity.content

data class ContentReport(
    val mapId: String,
    val mapVersion: Int,
    val width: Int,
    val depth: Int,
    val cellCounts: Map<String, Int>,
    val heightCounts: Map<Int, Int>,
    val materialCellCounts: Map<String, Int>,
    val entityTypeCounts: Map<String, Int>,
    val reachableCellCount: Int,
    val debugFootprint: String,
)
