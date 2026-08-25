package com.asciicity.entity

import com.asciicity.world.EntityId

data class Npc(
    val entityId: EntityId,
    val spriteId: String,
    val facingDegrees: Double,
    val displayName: String,
    val dialogueId: String,
) {
    init {
        require(spriteId.isNotBlank()) { "NPC sprite ID must not be blank" }
        require(facingDegrees >= 0.0 && facingDegrees < 360.0 && facingDegrees.isFinite()) {
            "NPC facing degrees must be finite, at least zero, and less than 360"
        }
        require(displayName.isNotBlank()) { "NPC display name must not be blank" }
        require(dialogueId.isNotBlank()) { "NPC dialogue ID must not be blank" }
    }
}
