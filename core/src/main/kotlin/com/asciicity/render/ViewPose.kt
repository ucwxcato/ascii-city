package com.asciicity.render

import com.asciicity.world.WorldPosition
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class ViewPose(
    val position: WorldPosition,
    val eyeHeight: Double,
    val yawRadians: Double,
    val pitchRadians: Double,
    val horizontalFovRadians: Double,
) {
    init {
        require(eyeHeight > 0.0 && eyeHeight.isFinite()) { "Eye height must be positive and finite" }
        require(yawRadians.isFinite()) { "Yaw must be finite" }
        require(pitchRadians.isFinite() && pitchRadians in -MAX_PITCH_RADIANS..MAX_PITCH_RADIANS) {
            "Pitch must be finite and between -55 and 55 degrees"
        }
        require(horizontalFovRadians.isFinite() && horizontalFovRadians in MIN_FOV_RADIANS..MAX_FOV_RADIANS) {
            "Horizontal FOV must be between 30 and 120 degrees"
        }
    }

    val forwardX: Double
        get() = sin(yawRadians)

    val forwardZ: Double
        get() = cos(yawRadians)

    val rightX: Double
        get() = cos(yawRadians)

    val rightZ: Double
        get() = -sin(yawRadians)

    fun withLook(yawRadians: Double = this.yawRadians, pitchRadians: Double = this.pitchRadians): ViewPose = copy(
        yawRadians = normalizeYaw(yawRadians),
        pitchRadians = pitchRadians.coerceIn(-MAX_PITCH_RADIANS, MAX_PITCH_RADIANS),
    )

    companion object {
        const val DEFAULT_FOV_DEGREES = 75.0
        const val MAX_PITCH_DEGREES = 55.0
        private val FULL_TURN = PI * 2.0
        val DEFAULT_FOV_RADIANS = Math.toRadians(DEFAULT_FOV_DEGREES)
        val MAX_PITCH_RADIANS = Math.toRadians(MAX_PITCH_DEGREES)
        val MIN_FOV_RADIANS = Math.toRadians(30.0)
        val MAX_FOV_RADIANS = Math.toRadians(120.0)

        fun normalizeYaw(yawRadians: Double): Double {
            require(yawRadians.isFinite()) { "Yaw must be finite" }
            val wrapped = yawRadians % FULL_TURN
            val normalized = if (wrapped < 0.0) wrapped + FULL_TURN else wrapped
            return if (normalized >= FULL_TURN || FULL_TURN - normalized < WRAP_EPSILON) 0.0 else normalized
        }

        fun interpolate(previous: ViewPose, current: ViewPose, alpha: Double): ViewPose {
            require(alpha.isFinite() && alpha in 0.0..1.0) { "Interpolation alpha must be between 0 and 1" }
            var yawDelta = normalizeYaw(current.yawRadians) - normalizeYaw(previous.yawRadians)
            if (yawDelta > PI) yawDelta -= FULL_TURN
            if (yawDelta < -PI) yawDelta += FULL_TURN
            return ViewPose(
                position = WorldPosition(
                    x = previous.position.x + (current.position.x - previous.position.x) * alpha,
                    z = previous.position.z + (current.position.z - previous.position.z) * alpha,
                ),
                eyeHeight = previous.eyeHeight + (current.eyeHeight - previous.eyeHeight) * alpha,
                yawRadians = normalizeYaw(previous.yawRadians + yawDelta * alpha),
                pitchRadians = previous.pitchRadians + (current.pitchRadians - previous.pitchRadians) * alpha,
                horizontalFovRadians = previous.horizontalFovRadians +
                    (current.horizontalFovRadians - previous.horizontalFovRadians) * alpha,
            )
        }

        private const val WRAP_EPSILON = 1e-12
    }
}
