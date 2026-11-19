package com.asciicity.render

import com.asciicity.content.AsciiSpriteRegistry
import com.asciicity.entity.WorldEntity
import com.asciicity.entity.WorldEntityType
import com.asciicity.world.EntityId
import com.asciicity.world.GameWorld

/** Renders authored door targets as full width entrance billboards. */
class DoorBillboardRenderer(
    private val world: GameWorld,
    sprites: AsciiSpriteRegistry,
    private val renderer: EntitySpriteRenderer = EntitySpriteRenderer(),
) {
    private val doorSprite = sprites.sprite(DOOR_SPRITE_ID)
    private var doorIds = emptyArray<EntityId>()
    private var resolvedDoors = emptyArray<WorldEntity>()
    private var observedEntityRevision = -1L

    fun render(frame: AsciiFrame, pose: ViewPose, metrics: ProjectionMetrics): Int {
        refreshEntitiesIfChanged()
        var writtenCells = 0
        for (door in resolvedDoors) {
            writtenCells += renderer.render(frame, pose, metrics, doorSprite, door.position)
        }
        return writtenCells
    }

    private fun refreshEntitiesIfChanged() {
        if (world.entityRevision == observedEntityRevision) return
        val doors = world.allEntities()
            .asSequence()
            .filter { it.type == WorldEntityType.DOOR && it.renderBillboard }
            .sortedBy { it.id }
            .toList()
        doorIds = doors.map(WorldEntity::id).toTypedArray()
        resolvedDoors = Array(doorIds.size) { index ->
            requireNotNull(world.entity(doorIds[index])) {
                "Door '${doorIds[index]}' disappeared while refreshing render state"
            }
        }
        observedEntityRevision = world.entityRevision
    }

    companion object {
        const val DOOR_SPRITE_ID = "sprite.door"
    }
}
