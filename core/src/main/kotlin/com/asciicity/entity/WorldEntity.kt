package com.asciicity.entity

import com.asciicity.world.EntityId
import com.asciicity.world.WorldPosition

enum class WorldEntityType {
    NPC,
    AMBIENT_ACTOR,
    DOOR,
    TERMINAL,
    LAMP,
}

data class WorldEntity(
    val id: EntityId,
    val type: WorldEntityType,
    val position: WorldPosition,
    val blocking: Boolean,
    val interactionId: String?,
    val displayText: String?,
    val collisionRadius: Double = 0.28,
    val destinationMapId: String? = null,
    val destinationPosition: WorldPosition? = null,
    val destinationYawDegrees: Double? = null,
    val renderBillboard: Boolean = false,
) {
    init {
        require(collisionRadius >= 0.0 && collisionRadius.isFinite()) {
            "Entity collision radius must be finite and non-negative"
        }
        require(destinationPosition == null || destinationMapId != null) {
            "A destination position requires a destination map"
        }
        require(destinationYawDegrees == null || destinationPosition != null) {
            "A destination yaw requires a destination position"
        }
    }
}
