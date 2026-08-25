package com.asciicity.system

import com.asciicity.content.AsciiSpriteRegistry
import com.asciicity.entity.Npc
import com.asciicity.entity.WorldEntity
import com.asciicity.entity.WorldEntityType
import com.asciicity.render.AsciiFrame
import com.asciicity.render.EntitySpriteRenderer
import com.asciicity.render.ProjectionMetrics
import com.asciicity.render.ViewPose
import com.asciicity.world.EntityId
import com.asciicity.world.GameWorld

class NpcSystem(
    private val world: GameWorld,
    npcs: Collection<Npc>,
    private val sprites: AsciiSpriteRegistry,
    private val renderer: EntitySpriteRenderer = EntitySpriteRenderer(),
) {
    private val orderedNpcs: Array<Npc> = npcs.sortedBy { it.entityId }.toTypedArray()
    private val resolvedEntities: Array<WorldEntity>
    private var observedEntityRevision: Long

    val npcCount: Int
        get() = orderedNpcs.size

    init {
        val ids = mutableSetOf<EntityId>()
        var index = 0
        while (index < orderedNpcs.size) {
            val npc = orderedNpcs[index]
            require(ids.add(npc.entityId)) { "Duplicate NPC definition '${npc.entityId}'" }
            require(sprites.contains(npc.spriteId)) {
                "NPC '${npc.entityId}' references unknown sprite '${npc.spriteId}'"
            }
            index += 1
        }
        resolvedEntities = Array(orderedNpcs.size) { npcIndex -> resolveWorldEntity(orderedNpcs[npcIndex]) }
        observedEntityRevision = world.entityRevision
    }

    fun render(frame: AsciiFrame, pose: ViewPose, metrics: ProjectionMetrics): Int {
        refreshEntitiesIfChanged()
        var writtenCells = 0
        var index = 0
        while (index < orderedNpcs.size) {
            val npc = orderedNpcs[index]
            val entity = resolvedEntities[index]
            writtenCells += renderer.render(
                frame = frame,
                pose = pose,
                metrics = metrics,
                sprite = sprites.sprite(npc.spriteId),
                position = entity.position,
            )
            index += 1
        }
        return writtenCells
    }

    internal fun entityIdAt(index: Int): EntityId = orderedNpcs[index].entityId

    private fun refreshEntitiesIfChanged() {
        val currentRevision = world.entityRevision
        if (currentRevision == observedEntityRevision) return
        var index = 0
        while (index < orderedNpcs.size) {
            resolvedEntities[index] = resolveWorldEntity(orderedNpcs[index])
            index += 1
        }
        observedEntityRevision = currentRevision
    }

    private fun resolveWorldEntity(npc: Npc): WorldEntity {
        val entity = requireNotNull(world.entity(npc.entityId)) {
            "NPC definition '${npc.entityId}' has no world entity"
        }
        require(entity.type == WorldEntityType.NPC) {
            "NPC definition '${npc.entityId}' must reference an NPC world entity"
        }
        require(entity.displayText == npc.displayName) {
            "NPC definition '${npc.entityId}' display name disagrees with its world entity"
        }
        require(entity.interactionId == npc.dialogueId) {
            "NPC definition '${npc.entityId}' dialogue disagrees with its world entity"
        }
        return entity
    }
}
