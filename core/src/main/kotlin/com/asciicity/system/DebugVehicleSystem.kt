package com.asciicity.system

import com.asciicity.entity.PlayerState
import com.asciicity.input.GameAction
import com.asciicity.input.InputController
import com.asciicity.render.ViewPose
import com.asciicity.world.WorldPosition
import kotlin.math.cos
import kotlin.math.sin

/**
 * Debug-only car controls. The vehicle occupies the player's semantic position, so its movement
 * remains deterministic and uses the same semantic collision as on-foot movement.
 */
class DebugVehicleSystem(
    private val collision: CollisionSystem,
    private val onFootRadius: Double,
    private val onFootEyeHeight: Double,
    private val movementSpeed: Double = 7.0,
    private val turnSpeedRadians: Double = Math.toRadians(115.0),
) {
    var isDriving: Boolean = false
        private set

    init {
        require(onFootRadius > 0.0 && onFootRadius.isFinite()) { "On-foot radius must be positive and finite" }
        require(onFootEyeHeight > 0.0 && onFootEyeHeight.isFinite()) { "On-foot eye height must be positive and finite" }
        require(movementSpeed > 0.0 && movementSpeed.isFinite()) { "Vehicle speed must be positive and finite" }
        require(turnSpeedRadians > 0.0 && turnSpeedRadians.isFinite()) { "Vehicle turn speed must be positive and finite" }
    }

    fun toggle(player: PlayerState): PlayerState? = if (isDriving) {
        isDriving = false
        player.copy(radius = onFootRadius, eyeHeight = onFootEyeHeight)
    } else if (collision.isPositionFree(player.position, VEHICLE_RADIUS)) {
        isDriving = true
        player.copy(radius = VEHICLE_RADIUS)
    } else {
        null
    }

    fun update(player: PlayerState, input: InputController, deltaSeconds: Double): PlayerState {
        require(deltaSeconds >= 0.0 && deltaSeconds.isFinite()) { "Vehicle delta must be finite and non-negative" }
        if (!isDriving) return player

        val throttle = axis(input, GameAction.MOVE_FORWARD, GameAction.MOVE_BACKWARD)
        val steering = axis(input, GameAction.STRAFE_RIGHT, GameAction.STRAFE_LEFT)
        if (throttle == 0.0) return player

        val yaw = ViewPose.normalizeYaw(player.yawRadians + steering * throttle * turnSpeedRadians * deltaSeconds)
        val distance = throttle * movementSpeed * deltaSeconds
        val next = collision.move(
            start = player.position,
            deltaX = sin(yaw) * distance,
            deltaZ = cos(yaw) * distance,
            radius = VEHICLE_RADIUS,
        )
        return player.copy(position = next, yawRadians = yaw)
    }

    fun thirdPersonPose(player: ViewPose): ViewPose {
        require(isDriving) { "Third-person vehicle pose is only available while driving" }
        val cameraPosition = collision.move(
            start = player.position,
            deltaX = -sin(player.yawRadians) * CAMERA_DISTANCE,
            deltaZ = -cos(player.yawRadians) * CAMERA_DISTANCE,
            radius = CAMERA_RADIUS,
        )
        return ViewPose(
            position = cameraPosition,
            eyeHeight = CAMERA_EYE_HEIGHT,
            yawRadians = player.yawRadians,
            pitchRadians = CAMERA_PITCH_RADIANS,
            horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
        )
    }

    private fun axis(input: InputController, positive: GameAction, negative: GameAction): Double =
        (if (input.isHeld(positive)) 1.0 else 0.0) - (if (input.isHeld(negative)) 1.0 else 0.0)

    companion object {
        const val SPRITE_ID = "sprite.car.sedan"
        const val VEHICLE_RADIUS = 0.42
        private const val CAMERA_RADIUS = 0.10
        private const val CAMERA_DISTANCE = 2.6
        private const val CAMERA_EYE_HEIGHT = 1.35
        private val CAMERA_PITCH_RADIANS = Math.toRadians(-8.0)
    }
}
