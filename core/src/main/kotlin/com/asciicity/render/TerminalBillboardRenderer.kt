package com.asciicity.render

import com.asciicity.content.AsciiSpriteRegistry
import com.asciicity.entity.WorldEntity
import com.asciicity.entity.WorldEntityType
import com.asciicity.world.EntityId
import com.asciicity.world.GameWorld

/** Renders the small shared screen-and-stand sprite for authored terminal entities. */
class TerminalBillboardRenderer(
    private val world: GameWorld,
    sprites: AsciiSpriteRegistry,
    private val renderer: EntitySpriteRenderer = EntitySpriteRenderer(),
) {
    private val terminalSprite = sprites.sprite(TERMINAL_SPRITE_ID)
    private var terminalIds = emptyArray<EntityId>()
    private var resolvedTerminals = emptyArray<WorldEntity>()
    private var observedEntityRevision = -1L

    fun render(frame: AsciiFrame, pose: ViewPose, metrics: ProjectionMetrics): Int {
        refreshEntitiesIfChanged()
        var writtenCells = 0
        var index = 0
        while (index < resolvedTerminals.size) {
            writtenCells += renderer.render(
                frame = frame,
                pose = pose,
                metrics = metrics,
                sprite = terminalSprite,
                position = resolvedTerminals[index].position,
            )
            index += 1
        }
        return writtenCells
    }

    private fun refreshEntitiesIfChanged() {
        val currentRevision = world.entityRevision
        if (currentRevision == observedEntityRevision) return
        val terminals = world.allEntities()
            .asSequence()
            .filter { it.type == WorldEntityType.TERMINAL }
            .sortedBy { it.id }
            .toList()
        terminalIds = terminals.map(WorldEntity::id).toTypedArray()
        resolvedTerminals = Array(terminalIds.size) { index ->
            requireNotNull(world.entity(terminalIds[index])) {
                "Terminal '${terminalIds[index]}' disappeared while refreshing render state"
            }.also { entity ->
                require(entity.type == WorldEntityType.TERMINAL) {
                    "Terminal '${entity.id}' changed to ${entity.type} while refreshing render state"
                }
            }
        }
        observedEntityRevision = currentRevision
    }

    companion object {
        const val TERMINAL_SPRITE_ID = "sprite.terminal"
    }
}
