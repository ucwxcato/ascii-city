package com.asciicity.system

import com.asciicity.entity.WorldEntity
import com.asciicity.entity.WorldEntityType
import com.asciicity.render.AsciiFontProfile
import com.asciicity.render.GridRaycaster
import com.asciicity.render.ProjectionMetrics
import com.asciicity.render.ViewPose
import com.asciicity.world.EntityId
import com.asciicity.world.GameWorld
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class InteractionTarget(
    val entityId: EntityId,
    val interactionId: String,
    val label: String,
    val depth: Double,
)

sealed interface InteractionOutcome {
    data class ShowMessage(val title: String, val message: String) : InteractionOutcome
    data class StartDialogue(val dialogueId: String) : InteractionOutcome
    data class EnterMap(
        val mapId: String,
        val arrivalPosition: com.asciicity.world.WorldPosition? = null,
        val arrivalYawDegrees: Double? = null,
    ) : InteractionOutcome
    data class Error(val message: String) : InteractionOutcome
}

class InteractionSystem(
    private val world: GameWorld,
    private val maximumRange: Double = DEFAULT_MAXIMUM_RANGE,
) {
    init {
        require(maximumRange > 0.0 && maximumRange.isFinite()) {
            "Interaction range must be positive and finite"
        }
    }

    fun findTarget(
        pose: ViewPose,
        columns: Int,
        rows: Int,
        fontProfile: AsciiFontProfile = AsciiFontProfile.STANDARD,
    ): InteractionTarget? {
        val metrics = ProjectionMetrics(
            columns = columns,
            rows = rows,
            pixelWidth = columns * fontProfile.cellWidth,
            pixelHeight = rows * fontProfile.cellHeight,
            horizontalFovRadians = pose.horizontalFovRadians,
            pitchRadians = pose.pitchRadians,
        )
        val centerColumn = columns / 2
        val centerRow = rows / 2
        val rawRayX = metrics.rayDirectionX(pose, centerColumn)
        val rawRayZ = metrics.rayDirectionZ(pose, centerColumn)
        val rayLength = hypot(rawRayX, rawRayZ)
        val rayX = rawRayX / rayLength
        val rayZ = rawRayZ / rayLength

        var nearest: InteractionTarget? = null
        for (entity in world.allEntities()) {
            val interactionId = entity.interactionId?.takeIf(String::isNotBlank) ?: continue
            if (entity.type == WorldEntityType.LAMP) continue
            val deltaX = entity.position.x - pose.position.x
            val deltaZ = entity.position.z - pose.position.z
            val distance = hypot(deltaX, deltaZ)
            if (distance > maximumRange || distance <= MINIMUM_DEPTH) continue

            val forwardDepth = deltaX * pose.forwardX + deltaZ * pose.forwardZ
            if (forwardDepth <= MINIMUM_DEPTH) continue
            val alongRay = deltaX * rayX + deltaZ * rayZ
            if (alongRay <= MINIMUM_DEPTH) continue
            val perpendicularSquared = (deltaX * deltaX + deltaZ * deltaZ - alongRay * alongRay).coerceAtLeast(0.0)
            val targetRadius = max(entity.collisionRadius, MINIMUM_TARGET_RADIUS)
            if (sqrt(perpendicularSquared) > targetRadius) continue
            if (!reticleIntersects(entity, pose, metrics, forwardDepth, centerRow)) continue
            if (worldOccludes(pose, metrics, rayX, rayZ, forwardDepth - targetRadius, centerRow)) continue

            val currentNearest = nearest
            if (currentNearest == null || forwardDepth < currentNearest.depth) {
                nearest = InteractionTarget(
                    entityId = entity.id,
                    interactionId = interactionId,
                    label = entity.displayText?.takeIf(String::isNotBlank) ?: entity.type.name,
                    depth = forwardDepth,
                )
            }
        }
        return nearest
    }

    fun interact(target: InteractionTarget): InteractionOutcome {
        val entity = world.entity(target.entityId)
            ?: return InteractionOutcome.Error("TARGET NO LONGER AVAILABLE")
        val interactionId = entity.interactionId
        if (interactionId.isNullOrBlank() || interactionId != target.interactionId) {
            return InteractionOutcome.Error("TARGET DATA IS INVALID")
        }
        return when (entity.type) {
            WorldEntityType.TERMINAL,
            WorldEntityType.NPC -> InteractionOutcome.StartDialogue(interactionId)
            WorldEntityType.DOOR -> entity.destinationMapId?.let {
                InteractionOutcome.EnterMap(it, entity.destinationPosition, entity.destinationYawDegrees)
            }
                ?: InteractionOutcome.ShowMessage(
                    title = entity.displayText?.takeIf(String::isNotBlank) ?: "DOOR",
                    message = "ACCESS DENIED\nSEAL CONTROL OFFLINE",
                )
            WorldEntityType.LAMP,
            WorldEntityType.AMBIENT_ACTOR -> InteractionOutcome.Error("TARGET IS NOT INTERACTIVE")
        }
    }

    private fun reticleIntersects(
        entity: WorldEntity,
        pose: ViewPose,
        metrics: ProjectionMetrics,
        depth: Double,
        centerRow: Int,
    ): Boolean {
        val bottom = metrics.projectWorldHeight(0.0, pose.eyeHeight, depth)
        val top = metrics.projectWorldHeight(interactionHeight(entity.type), pose.eyeHeight, depth)
        val reticle = centerRow + 0.5
        return reticle in min(bottom, top)..max(bottom, top)
    }

    private fun worldOccludes(
        pose: ViewPose,
        metrics: ProjectionMetrics,
        rayX: Double,
        rayZ: Double,
        maximumTargetDepth: Double,
        centerRow: Int,
    ): Boolean {
        if (maximumTargetDepth <= MINIMUM_DEPTH) return false
        val forwardScale = rayX * pose.forwardX + rayZ * pose.forwardZ
        var occluded = false
        GridRaycaster.traverse(
            originX = pose.position.x,
            originZ = pose.position.z,
            directionX = rayX,
            directionZ = rayZ,
            gridWidth = world.grid.width,
            gridDepth = world.grid.depth,
            maxDistance = maximumRange,
            maxSteps = MAXIMUM_RAY_STEPS,
        ) { cellX, cellZ, _, distance, _ ->
            val perpendicularDepth = distance * forwardScale
            if (perpendicularDepth >= maximumTargetDepth) return@traverse false
            val definition = world.grid.cellAt(cellX, cellZ)
            if (!definition.opaque || definition.height <= 0) return@traverse true
            val bottom = metrics.projectWorldHeight(0.0, pose.eyeHeight, perpendicularDepth)
            val top = metrics.projectWorldHeight(definition.height.toDouble(), pose.eyeHeight, perpendicularDepth)
            val reticle = centerRow + 0.5
            if (reticle in min(bottom, top)..max(bottom, top)) {
                occluded = true
                false
            } else {
                true
            }
        }
        return occluded
    }

    private fun interactionHeight(type: WorldEntityType): Double = when (type) {
        WorldEntityType.NPC -> 1.75
        WorldEntityType.AMBIENT_ACTOR -> 1.75
        WorldEntityType.DOOR -> 2.25
        WorldEntityType.TERMINAL -> 1.25
        WorldEntityType.LAMP -> 2.5
    }

    companion object {
        const val DEFAULT_MAXIMUM_RANGE = 1.75
        private const val MINIMUM_TARGET_RADIUS = 0.18
        private const val MINIMUM_DEPTH = 1e-6
        private const val MAXIMUM_RAY_STEPS = 32
    }
}
