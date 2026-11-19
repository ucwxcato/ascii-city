package com.asciicity.system

import com.asciicity.TestContent
import com.asciicity.content.CellRegistry
import com.asciicity.entity.PlayerState
import com.asciicity.entity.WorldEntity
import com.asciicity.world.CellDefinition
import com.asciicity.world.GameWorld
import com.asciicity.world.GridCell
import com.asciicity.world.WorldGrid
import com.asciicity.world.WorldPosition

internal object SystemTestWorld {
    fun create(
        width: Int = 10,
        depth: Int = 10,
        playerPosition: WorldPosition = WorldPosition(2.5, 2.5),
        solidCells: Set<GridCell> = emptySet(),
        entities: List<WorldEntity> = emptyList(),
    ): GameWorld {
        val materials = TestContent.loaded.materials
        val cells = CellRegistry.create(
            listOf(
                CellDefinition(
                    id = "test.open",
                    token = '.',
                    groundMaterialId = "ground.road",
                    facadeMaterialId = "facade.low",
                    solid = false,
                    opaque = false,
                    height = 0,
                    tags = emptySet(),
                    interactionType = null,
                ),
                CellDefinition(
                    id = "test.wall",
                    token = '#',
                    groundMaterialId = "ground.road",
                    facadeMaterialId = "facade.low",
                    solid = true,
                    opaque = true,
                    height = 1,
                    tags = emptySet(),
                    interactionType = null,
                ),
            ),
            materials,
            "test",
        )
        val indices = IntArray(width * depth)
        for (cell in solidCells) indices[cell.z * width + cell.x] = 1
        val grid = WorldGrid.fromResolvedIndices(width, depth, cells, indices)
        return GameWorld(
            mapId = "test",
            mapVersion = 1,
            grid = grid,
            player = PlayerState(playerPosition, 0.0),
            entities = entities,
        )
    }
}
