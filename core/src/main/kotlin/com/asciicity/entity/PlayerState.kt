package com.asciicity.entity

import com.asciicity.world.WorldPosition

data class PlayerState(
    val position: WorldPosition,
    val yawRadians: Double,
    val pitchRadians: Double = 0.0,
    val radius: Double = 0.22,
    val eyeHeight: Double = 0.60,
) {
    init {
        require(yawRadians.isFinite()) { "Player yaw must be finite" }
        require(pitchRadians.isFinite()) { "Player pitch must be finite" }
        require(radius > 0.0 && radius.isFinite()) { "Player radius must be positive and finite" }
        require(eyeHeight > 0.0 && eyeHeight.isFinite()) { "Player eye height must be positive and finite" }
    }
}
