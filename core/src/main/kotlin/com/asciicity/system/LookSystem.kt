package com.asciicity.system

import com.asciicity.entity.PlayerState
import com.asciicity.input.GameAction
import com.asciicity.input.InputController
import com.asciicity.render.ViewPose

class LookSystem(
    private val yawSpeedRadians: Double = Math.toRadians(100.0),
    private val pitchSpeedRadians: Double = Math.toRadians(75.0),
) {
    init {
        require(yawSpeedRadians > 0.0 && yawSpeedRadians.isFinite()) { "Yaw speed must be positive and finite" }
        require(pitchSpeedRadians > 0.0 && pitchSpeedRadians.isFinite()) { "Pitch speed must be positive and finite" }
    }

    fun update(player: PlayerState, input: InputController, deltaSeconds: Double): PlayerState {
        require(deltaSeconds >= 0.0 && deltaSeconds.isFinite()) { "Look delta must be finite and non-negative" }
        val yawAxis = axis(input, GameAction.LOOK_RIGHT, GameAction.LOOK_LEFT)
        val pitchAxis = axis(input, GameAction.LOOK_UP, GameAction.LOOK_DOWN)
        if (yawAxis == 0.0 && pitchAxis == 0.0) return player
        return player.copy(
            yawRadians = ViewPose.normalizeYaw(player.yawRadians + yawAxis * yawSpeedRadians * deltaSeconds),
            pitchRadians = (player.pitchRadians + pitchAxis * pitchSpeedRadians * deltaSeconds)
                .coerceIn(-ViewPose.MAX_PITCH_RADIANS, ViewPose.MAX_PITCH_RADIANS),
        )
    }

    private fun axis(input: InputController, positive: GameAction, negative: GameAction): Double =
        (if (input.isHeld(positive)) 1.0 else 0.0) - (if (input.isHeld(negative)) 1.0 else 0.0)
}
