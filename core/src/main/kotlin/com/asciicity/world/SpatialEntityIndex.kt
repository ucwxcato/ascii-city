package com.asciicity.world

import com.asciicity.entity.WorldEntity

internal class SpatialEntityIndex {
    private val cellByEntity = mutableMapOf<EntityId, GridCell>()
    private val entitiesByCell = mutableMapOf<GridCell, MutableSet<EntityId>>()

    fun add(entity: WorldEntity) {
        check(entity.id !in cellByEntity) { "Entity '${entity.id}' already exists in the spatial index" }
        val cell = entity.position.toGridCell()
        cellByEntity[entity.id] = cell
        entitiesByCell.getOrPut(cell, ::linkedSetOf).add(entity.id)
    }

    fun move(entity: WorldEntity) {
        val previousCell = cellByEntity[entity.id]
            ?: throw IllegalStateException("Entity '${entity.id}' does not exist in the spatial index")
        val nextCell = entity.position.toGridCell()
        if (previousCell == nextCell) {
            return
        }
        entitiesByCell[previousCell]?.let { ids ->
            ids.remove(entity.id)
            if (ids.isEmpty()) {
                entitiesByCell.remove(previousCell)
            }
        }
        cellByEntity[entity.id] = nextCell
        entitiesByCell.getOrPut(nextCell, ::linkedSetOf).add(entity.id)
    }

    fun remove(id: EntityId) {
        val cell = cellByEntity.remove(id) ?: return
        entitiesByCell[cell]?.let { ids ->
            ids.remove(id)
            if (ids.isEmpty()) {
                entitiesByCell.remove(cell)
            }
        }
    }

    fun idsAt(cell: GridCell): Set<EntityId> = entitiesByCell[cell]?.toSet().orEmpty()
}
