package com.asciicity.system

import com.asciicity.entity.PlayerState
import com.asciicity.input.GameAction
import com.asciicity.input.InputController
import com.asciicity.world.WorldPosition
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class MovementSystem(
    private val collision: CollisionSystem,
    private val movementSpeed: Double = 3.0,
) {
    init {
        require(movementSpeed > 0.0 && movementSpeed.isFinite()) { "Movement speed must be positive and finite" }
    }

    fun update(player: PlayerState, input: InputController, deltaSeconds: Double): PlayerState {
        require(deltaSeconds >= 0.0 && deltaSeconds.isFinite()) { "Movement delta must be finite and non-negative" }
        var forwardAxis = axis(input, GameAction.MOVE_FORWARD, GameAction.MOVE_BACKWARD)
        var strafeAxis = axis(input, GameAction.STRAFE_RIGHT, GameAction.STRAFE_LEFT)
        val magnitude = hypot(forwardAxis, strafeAxis)
        if (magnitude > 1.0) {
            forwardAxis /= magnitude
            strafeAxis /= magnitude
        }
        if (forwardAxis == 0.0 && strafeAxis == 0.0) return player

        val forwardX = sin(player.yawRadians)
        val forwardZ = cos(player.yawRadians)
        val rightX = cos(player.yawRadians)
        val rightZ = -sin(player.yawRadians)
        val distance = movementSpeed * deltaSeconds
        val next = collision.move(
            start = player.position,
            deltaX = (forwardX * forwardAxis + rightX * strafeAxis) * distance,
            deltaZ = (forwardZ * forwardAxis + rightZ * strafeAxis) * distance,
            radius = player.radius,
        )
        return if (next == player.position) player else player.copy(position = WorldPosition(next.x, next.z))
    }

    private fun axis(input: InputController, positive: GameAction, negative: GameAction): Double =
        (if (input.isHeld(positive)) 1.0 else 0.0) - (if (input.isHeld(negative)) 1.0 else 0.0)
}
