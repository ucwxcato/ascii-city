package com.asciicity.entity

import com.asciicity.world.EntityId
import com.asciicity.world.WorldPosition

data class AmbientActor(
    val entityId: EntityId,
    val spriteId: String,
    val speedCellsPerSecond: Double,
    val waitSeconds: Double,
    val startDelaySeconds: Double,
    val waypoints: List<WorldPosition>,
) {
    init {
        require(spriteId.isNotBlank()) { "Ambient actor sprite ID must not be blank" }
        require(speedCellsPerSecond in MINIMUM_SPEED..MAXIMUM_SPEED && speedCellsPerSecond.isFinite()) {
            "Ambient actor speed must be between $MINIMUM_SPEED and $MAXIMUM_SPEED"
        }
        require(waitSeconds in 0.0..MAXIMUM_WAIT && waitSeconds.isFinite()) {
            "Ambient actor wait must be between zero and $MAXIMUM_WAIT seconds"
        }
        require(startDelaySeconds in 0.0..MAXIMUM_START_DELAY && startDelaySeconds.isFinite()) {
            "Ambient actor start delay must be between zero and $MAXIMUM_START_DELAY seconds"
        }
        require(waypoints.size in MINIMUM_WAYPOINTS..MAXIMUM_WAYPOINTS) {
            "Ambient actor route must contain between $MINIMUM_WAYPOINTS and $MAXIMUM_WAYPOINTS waypoints"
        }
    }

    companion object {
        const val MAXIMUM_ACTORS = 10
        const val MINIMUM_WAYPOINTS = 2
        const val MAXIMUM_WAYPOINTS = 16
        const val MINIMUM_SPEED = 0.2
        const val MAXIMUM_SPEED = 0.8
        const val MAXIMUM_WAIT = 12.0
        const val MAXIMUM_START_DELAY = 30.0
        const val MAXIMUM_SEGMENT_LENGTH = 12.0
    }
}
