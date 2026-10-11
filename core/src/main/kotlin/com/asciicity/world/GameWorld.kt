package com.asciicity.world

import com.asciicity.entity.PlayerState
import com.asciicity.entity.WorldEntity

class GameWorld(
    val mapId: String,
    val mapVersion: Int,
    val grid: WorldGrid,
    player: PlayerState,
    entities: Collection<WorldEntity>,
    worldAssets: Collection<WorldAsset> = emptyList(),
) {
    private val entitiesById = linkedMapOf<EntityId, WorldEntity>()
    private val spatialIndex = SpatialEntityIndex()
    private val worldAssetsById = linkedMapOf<String, WorldAsset>()

    var player: PlayerState = player
        private set

    var simulationTimeSeconds: Double = 0.0
        private set

    internal var entityRevision: Long = 0L
        private set

    init {
        require(mapId.isNotBlank()) { "Map ID must not be blank" }
        require(mapVersion > 0) { "Map version must be positive" }
        require(grid.contains(player.position.toGridCell())) { "Player must start inside the world grid" }
        for (entity in entities) {
            addEntity(entity)
        }
        for (asset in worldAssets) {
            addWorldAsset(asset)
        }
    }

    fun allEntities(): List<WorldEntity> = entitiesById.values.toList()

    fun allWorldAssets(): List<WorldAsset> = worldAssetsById.values.toList()

    fun entity(id: EntityId): WorldEntity? = entitiesById[id]

    fun entitiesAt(cell: GridCell): List<WorldEntity> = spatialIndex.idsAt(cell)
        .sorted()
        .mapNotNull(entitiesById::get)

    fun addEntity(entity: WorldEntity) {
        require(grid.contains(entity.position.toGridCell())) { "Entity '${entity.id}' is outside the world grid" }
        check(entity.id !in entitiesById) { "Entity '${entity.id}' already exists" }
        entitiesById[entity.id] = entity
        spatialIndex.add(entity)
        entityRevision += 1L
    }

    fun addWorldAsset(asset: WorldAsset) {
        require(grid.contains(asset.position.toGridCell())) { "World asset '${asset.id}' is outside the world grid" }
        check(asset.id !in worldAssetsById) { "World asset '${asset.id}' already exists" }
        worldAssetsById[asset.id] = asset
    }

    fun moveEntity(id: EntityId, position: WorldPosition) {
        require(grid.contains(position.toGridCell())) { "Entity '$id' cannot move outside the world grid" }
        val current = entitiesById[id] ?: throw IllegalArgumentException("Unknown entity '$id'")
        val moved = current.copy(position = position)
        entitiesById[id] = moved
        spatialIndex.move(moved)
        entityRevision += 1L
    }

    fun removeEntity(id: EntityId): WorldEntity? {
        val removed = entitiesById.remove(id) ?: return null
        spatialIndex.remove(id)
        entityRevision += 1L
        return removed
    }

    fun updatePlayer(next: PlayerState) {
        require(grid.contains(next.position.toGridCell())) { "Player cannot move outside the world grid" }
        player = next
    }

    fun advanceTime(deltaSeconds: Double) {
        require(deltaSeconds >= 0.0 && deltaSeconds.isFinite()) { "Simulation delta must be finite and non-negative" }
        simulationTimeSeconds += deltaSeconds
    }
}
