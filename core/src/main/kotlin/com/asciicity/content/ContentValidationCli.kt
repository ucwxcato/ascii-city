package com.asciicity.content

import java.nio.file.Path
import kotlin.system.exitProcess

fun main(arguments: Array<String>) {
    val assetRoot = Path.of(arguments.getOrElse(0) { "." })
    val mapName = arguments.getOrElse(1) { "city-alpha" }
    try {
        val content = ContentLoader().load(assetRoot, mapName)
        val report = ContentVerifier.verify(content)
        println(
            "Validated data/palette.json, data/presentation.json, data/materials.json, " +
                "data/cells.json, data/sprites.json, and data/dialogue.json",
        )
        println("Validated maps/$mapName.txt, maps/$mapName.meta.json, and maps/${content.metadata.objectsFile}")
        println(
            "World ${report.mapId} v${report.mapVersion}: ${report.width}x${report.depth}, " +
                "${report.cellCounts.values.sum()} cells, ${content.world.allEntities().size} entities, " +
                "${report.reachableCellCount} reachable cells",
        )
        println("Cell counts: ${report.cellCounts.entries.joinToString { "${it.key}=${it.value}" }}")
        println("Height counts: ${report.heightCounts.entries.joinToString { "${it.key}=${it.value}" }}")
        println("Entity counts: ${report.entityTypeCounts.entries.joinToString { "${it.key}=${it.value}" }}")
        println("Debug footprint (north at top, 2x2 cells per character):")
        println(report.debugFootprint)
    } catch (error: ContentValidationException) {
        System.err.println("Content validation failed: ${error.message}")
        exitProcess(1)
    }
}
