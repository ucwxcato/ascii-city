package com.asciicity.system

import com.asciicity.entity.PlayerState
import com.asciicity.input.GameAction
import com.asciicity.input.InputController
import com.asciicity.render.ViewPose
import com.asciicity.world.WorldPosition
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sign
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

    var isDrifting: Boolean = false
        private set

    var frontWheelSteerRadians: Double = 0.0
        private set

    var wheelRotationRadians: Double = 0.0
        private set

    private var velocityX = 0.0
    private var velocityZ = 0.0

    init {
        require(onFootRadius > 0.0 && onFootRadius.isFinite()) { "On-foot radius must be positive and finite" }
        require(onFootEyeHeight > 0.0 && onFootEyeHeight.isFinite()) { "On-foot eye height must be positive and finite" }
        require(movementSpeed > 0.0 && movementSpeed.isFinite()) { "Vehicle speed must be positive and finite" }
        require(turnSpeedRadians > 0.0 && turnSpeedRadians.isFinite()) { "Vehicle turn speed must be positive and finite" }
    }

    fun toggle(player: PlayerState): PlayerState? = if (isDriving) {
        isDriving = false
        resetDynamics()
        player.copy(radius = onFootRadius, eyeHeight = onFootEyeHeight)
    } else if (collision.isPositionFree(player.position, VEHICLE_RADIUS)) {
        isDriving = true
        resetDynamics()
        player.copy(radius = VEHICLE_RADIUS)
    } else {
        null
    }

    fun update(player: PlayerState, input: InputController, deltaSeconds: Double): PlayerState {
        require(deltaSeconds >= 0.0 && deltaSeconds.isFinite()) { "Vehicle delta must be finite and non-negative" }
        if (!isDriving) return player

        val throttle = axis(input, GameAction.MOVE_FORWARD, GameAction.MOVE_BACKWARD)
        val steering = axis(input, GameAction.STRAFE_RIGHT, GameAction.STRAFE_LEFT)
        val handbrake = input.isHeld(GameAction.HANDBRAKE)
        val forwardX = sin(player.yawRadians)
        val forwardZ = cos(player.yawRadians)
        val rightX = cos(player.yawRadians)
        val rightZ = -sin(player.yawRadians)
        var forwardVelocity = velocityX * forwardX + velocityZ * forwardZ
        var lateralVelocity = velocityX * rightX + velocityZ * rightZ
        val targetVelocity = when {
            throttle > 0.0 -> movementSpeed
            throttle < 0.0 -> -REVERSE_SPEED
            else -> 0.0
        }
        val rate = if (abs(targetVelocity) > abs(forwardVelocity)) ACCELERATION else BRAKE_DECELERATION
        forwardVelocity = approach(forwardVelocity, targetVelocity, rate * deltaSeconds)
        if (throttle == 0.0 && abs(forwardVelocity) < STOP_SPEED) forwardVelocity = 0.0

        val steeringScale = (abs(forwardVelocity) / movementSpeed).coerceIn(0.0, 1.0)
        frontWheelSteerRadians = steering * MAXIMUM_WHEEL_STEER_RADIANS * steeringScale
        val yaw = ViewPose.normalizeYaw(
            player.yawRadians + steering * sign(forwardVelocity) * turnSpeedRadians * steeringScale * deltaSeconds,
        )
        val driftingRequested = handbrake && abs(forwardVelocity) >= DRIFT_MINIMUM_SPEED && steering != 0.0
        val yawForwardX = sin(yaw)
        val yawForwardZ = cos(yaw)
        val yawRightX = cos(yaw)
        val yawRightZ = -sin(yaw)
        if (driftingRequested) {
            velocityX = velocityX * HAND_BRAKE_LATERAL_RETENTION + yawForwardX * forwardVelocity * HAND_BRAKE_FORWARD_BLEND
            velocityZ = velocityZ * HAND_BRAKE_LATERAL_RETENTION + yawForwardZ * forwardVelocity * HAND_BRAKE_FORWARD_BLEND
        } else {
            lateralVelocity *= NORMAL_LATERAL_RETENTION
            velocityX = yawForwardX * forwardVelocity + yawRightX * lateralVelocity
            velocityZ = yawForwardZ * forwardVelocity + yawRightZ * lateralVelocity
        }
        val next = collision.move(
            start = player.position,
            deltaX = velocityX * deltaSeconds,
            deltaZ = velocityZ * deltaSeconds,
            radius = VEHICLE_RADIUS,
        )
        velocityX = (next.x - player.position.x) / deltaSeconds.coerceAtLeast(MINIMUM_DELTA)
        velocityZ = (next.z - player.position.z) / deltaSeconds.coerceAtLeast(MINIMUM_DELTA)
        wheelRotationRadians += forwardVelocity * deltaSeconds / WHEEL_RADIUS
        val postLateralVelocity = velocityX * yawRightX + velocityZ * yawRightZ
        isDrifting = driftingRequested && abs(postLateralVelocity) >= DRIFT_MINIMUM_LATERAL_SPEED
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

    private fun resetDynamics() {
        velocityX = 0.0
        velocityZ = 0.0
        isDrifting = false
        frontWheelSteerRadians = 0.0
        wheelRotationRadians = 0.0
    }

    private fun approach(current: Double, target: Double, distance: Double): Double = when {
        current < target -> minOf(current + distance, target)
        current > target -> maxOf(current - distance, target)
        else -> current
    }

    companion object {
        const val SPRITE_ID = "sprite.car.sedan"
        const val VEHICLE_RADIUS = 0.42
        private const val REVERSE_SPEED = 2.75
        private const val ACCELERATION = 5.0
        private const val BRAKE_DECELERATION = 8.0
        private const val STOP_SPEED = 0.05
        private const val NORMAL_LATERAL_RETENTION = 0.25
        private const val HAND_BRAKE_LATERAL_RETENTION = 0.94
        private const val HAND_BRAKE_FORWARD_BLEND = 0.06
        private const val DRIFT_MINIMUM_SPEED = 3.5
        private const val DRIFT_MINIMUM_LATERAL_SPEED = 0.10
        private const val MINIMUM_DELTA = 1e-6
        private const val WHEEL_RADIUS = 0.24
        private val MAXIMUM_WHEEL_STEER_RADIANS = Math.toRadians(26.0)
        private const val CAMERA_RADIUS = 0.10
        private const val CAMERA_DISTANCE = 4.2
        private const val CAMERA_EYE_HEIGHT = 1.35
        private val CAMERA_PITCH_RADIANS = Math.toRadians(-8.0)
    }
}
