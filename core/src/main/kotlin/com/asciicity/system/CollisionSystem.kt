package com.asciicity.system

import com.asciicity.world.EntityId
import com.asciicity.world.GameWorld
import com.asciicity.world.WorldPosition
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot

class CollisionSystem(
    private val world: GameWorld,
    private val maximumSubstep: Double = 0.10,
) {
    init {
        require(maximumSubstep > 0.0 && maximumSubstep.isFinite()) {
            "Maximum collision substep must be positive and finite"
        }
    }

    fun isPositionFree(
        position: WorldPosition,
        radius: Double,
        ignoreEntityId: EntityId? = null,
    ): Boolean {
        require(radius > 0.0 && radius.isFinite()) { "Collision radius must be positive and finite" }
        if (
            position.x - radius < 0.0 || position.z - radius < 0.0 ||
            position.x + radius > world.grid.width || position.z + radius > world.grid.depth
        ) return false

        val minimumX = floor(position.x - radius).toInt()
        val maximumX = floor(position.x + radius).toInt()
        val minimumZ = floor(position.z - radius).toInt()
        val maximumZ = floor(position.z + radius).toInt()
        val radiusSquared = radius * radius
        for (z in minimumZ..maximumZ) {
            for (x in minimumX..maximumX) {
                if (!world.grid.cellAt(x, z).solid) continue
                val closestX = position.x.coerceIn(x.toDouble(), x + 1.0)
                val closestZ = position.z.coerceIn(z.toDouble(), z + 1.0)
                val deltaX = position.x - closestX
                val deltaZ = position.z - closestZ
                if (deltaX * deltaX + deltaZ * deltaZ < radiusSquared - COLLISION_EPSILON) return false
            }
        }

        for (entity in world.allEntities()) {
            if (!entity.blocking || entity.id == ignoreEntityId) continue
            val combinedRadius = radius + entity.collisionRadius
            val deltaX = position.x - entity.position.x
            val deltaZ = position.z - entity.position.z
            if (deltaX * deltaX + deltaZ * deltaZ < combinedRadius * combinedRadius - COLLISION_EPSILON) return false
        }
        return true
    }

    fun move(
        start: WorldPosition,
        deltaX: Double,
        deltaZ: Double,
        radius: Double,
        ignoreEntityId: EntityId? = null,
    ): WorldPosition {
        require(deltaX.isFinite() && deltaZ.isFinite()) { "Movement delta must be finite" }
        if (deltaX == 0.0 && deltaZ == 0.0) return start
        val stepCount = ceil(hypot(deltaX, deltaZ) / maximumSubstep).toInt().coerceAtLeast(1)
        val stepX = deltaX / stepCount
        val stepZ = deltaZ / stepCount
        var x = start.x
        var z = start.z
        repeat(stepCount) {
            val candidateX = WorldPosition(x + stepX, z)
            if (isPositionFree(candidateX, radius, ignoreEntityId)) x = candidateX.x
            val candidateZ = WorldPosition(x, z + stepZ)
            if (isPositionFree(candidateZ, radius, ignoreEntityId)) z = candidateZ.z
        }
        return WorldPosition(x, z)
    }

    companion object {
        private const val COLLISION_EPSILON = 1e-12
    }
}
