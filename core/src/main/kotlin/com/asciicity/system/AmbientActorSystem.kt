package com.asciicity.system

import com.asciicity.content.AsciiSpriteRegistry
import com.asciicity.entity.AmbientActor
import com.asciicity.entity.WorldEntity
import com.asciicity.entity.WorldEntityType
import com.asciicity.render.AsciiFrame
import com.asciicity.render.AsciiSprite
import com.asciicity.render.EntitySpriteRenderer
import com.asciicity.render.ProjectionMetrics
import com.asciicity.render.ViewPose
import com.asciicity.world.GameWorld
import com.asciicity.world.WorldPosition
import kotlin.math.hypot

class AmbientActorSystem(
    private val world: GameWorld,
    actors: Collection<AmbientActor>,
    sprites: AsciiSpriteRegistry,
    private val renderer: EntitySpriteRenderer = EntitySpriteRenderer(),
) {
    private val collision = CollisionSystem(world)
    private val states: Array<ActorState>
    private val actorSprites: Array<AsciiSprite>

    init {
        val ordered = actors.sortedBy(AmbientActor::entityId)
        require(ordered.map(AmbientActor::entityId).toSet().size == ordered.size) {
            "Ambient actor IDs must be unique"
        }
        states = Array(ordered.size) { index ->
            val actor = ordered[index]
            val entity = resolveEntity(actor)
            ActorState(actor, entity.position, actor.startDelaySeconds)
        }
        actorSprites = Array(ordered.size) { index ->
            val actor = ordered[index]
            require(sprites.contains(actor.spriteId)) {
                "Ambient actor '${actor.entityId}' references unknown sprite '${actor.spriteId}'"
            }
            sprites.sprite(actor.spriteId)
        }
    }

    val actorCount: Int
        get() = states.size

    fun step(deltaSeconds: Double) {
        require(deltaSeconds >= 0.0 && deltaSeconds.isFinite()) {
            "Ambient actor delta must be finite and non-negative"
        }
        for (state in states) {
            state.previousPosition = state.currentPosition
            if (state.waitRemaining > 0.0) {
                state.waitRemaining = (state.waitRemaining - deltaSeconds).coerceAtLeast(0.0)
                continue
            }
            val target = state.actor.waypoints[state.targetIndex]
            val deltaX = target.x - state.currentPosition.x
            val deltaZ = target.z - state.currentPosition.z
            val distance = hypot(deltaX, deltaZ)
            val maximumStep = state.actor.speedCellsPerSecond * deltaSeconds
            val candidate = if (distance <= maximumStep) {
                target
            } else {
                WorldPosition(
                    state.currentPosition.x + deltaX / distance * maximumStep,
                    state.currentPosition.z + deltaZ / distance * maximumStep,
                )
            }
            val entity = requireNotNull(world.entity(state.actor.entityId))
            if (!isClearOfPlayer(candidate, entity.collisionRadius) ||
                !collision.isPositionFree(candidate, entity.collisionRadius, state.actor.entityId)
            ) {
                continue
            }
            state.currentPosition = candidate
            world.moveEntity(state.actor.entityId, candidate)
            if (candidate == target) advanceTarget(state)
        }
    }

    fun render(frame: AsciiFrame, pose: ViewPose, metrics: ProjectionMetrics, alpha: Double): Int {
        val interpolation = alpha.coerceIn(0.0, 1.0)
        var writes = 0
        for (index in states.indices) {
            val state = states[index]
            val position = WorldPosition(
                state.previousPosition.x + (state.currentPosition.x - state.previousPosition.x) * interpolation,
                state.previousPosition.z + (state.currentPosition.z - state.previousPosition.z) * interpolation,
            )
            writes += renderer.render(frame, pose, metrics, actorSprites[index], position)
        }
        return writes
    }

    fun synchronize() {
        for (state in states) {
            val position = requireNotNull(world.entity(state.actor.entityId)).position
            state.previousPosition = position
            state.currentPosition = position
        }
    }

    internal fun positionAt(index: Int): WorldPosition = states[index].currentPosition

    private fun advanceTarget(state: ActorState) {
        val lastIndex = state.actor.waypoints.lastIndex
        if (state.direction > 0 && state.targetIndex == lastIndex) state.direction = -1
        else if (state.direction < 0 && state.targetIndex == 0) state.direction = 1
        state.targetIndex += state.direction
        state.waitRemaining = state.actor.waitSeconds
    }

    private fun isClearOfPlayer(position: WorldPosition, radius: Double): Boolean {
        val combinedRadius = radius + world.player.radius
        val deltaX = position.x - world.player.position.x
        val deltaZ = position.z - world.player.position.z
        return deltaX * deltaX + deltaZ * deltaZ >= combinedRadius * combinedRadius
    }

    private fun resolveEntity(actor: AmbientActor): WorldEntity {
        val entity = requireNotNull(world.entity(actor.entityId)) {
            "Ambient actor definition '${actor.entityId}' has no world entity"
        }
        require(entity.type == WorldEntityType.AMBIENT_ACTOR) {
            "Ambient actor '${actor.entityId}' must reference an ambient actor world entity"
        }
        require(entity.position == actor.waypoints.first()) {
            "Ambient actor '${actor.entityId}' must start at its first waypoint"
        }
        return entity
    }

    private class ActorState(
        val actor: AmbientActor,
        position: WorldPosition,
        startDelay: Double,
    ) {
        var previousPosition = position
        var currentPosition = position
        var targetIndex = 1
        var direction = 1
        var waitRemaining = startDelay
    }
}
