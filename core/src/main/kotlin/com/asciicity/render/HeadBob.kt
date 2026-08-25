package com.asciicity.render

import kotlin.math.PI
import kotlin.math.sin

class HeadBob(
    private val amplitude: Double = DEFAULT_AMPLITUDE,
    private val cycleDistance: Double = DEFAULT_CYCLE_DISTANCE,
    private val responsePerSecond: Double = DEFAULT_RESPONSE_PER_SECOND,
) {
    private var phase = 0.0
    private var weight = 0.0

    var verticalOffset: Double = 0.0
        private set

    init {
        require(amplitude >= 0.0 && amplitude.isFinite()) { "Head-bob amplitude must be finite and non-negative" }
        require(cycleDistance > 0.0 && cycleDistance.isFinite()) {
            "Head-bob cycle distance must be positive and finite"
        }
        require(responsePerSecond > 0.0 && responsePerSecond.isFinite()) {
            "Head-bob response must be positive and finite"
        }
    }

    fun update(distanceMoved: Double, deltaSeconds: Double) {
        require(distanceMoved >= 0.0 && distanceMoved.isFinite()) {
            "Head-bob movement distance must be finite and non-negative"
        }
        require(deltaSeconds >= 0.0 && deltaSeconds.isFinite()) {
            "Head-bob delta must be finite and non-negative"
        }
        val targetWeight = if (distanceMoved > MOVEMENT_EPSILON) 1.0 else 0.0
        val response = (responsePerSecond * deltaSeconds).coerceIn(0.0, 1.0)
        weight = approach(weight, targetWeight, response)
        if (distanceMoved > MOVEMENT_EPSILON) {
            phase = (phase + distanceMoved / cycleDistance * FULL_TURN) % FULL_TURN
        }
        verticalOffset = sin(phase) * amplitude * weight
    }

    fun apply(pose: ViewPose): ViewPose = if (verticalOffset == 0.0) {
        pose
    } else {
        pose.copy(eyeHeight = pose.eyeHeight + verticalOffset)
    }

    fun reset() {
        phase = 0.0
        weight = 0.0
        verticalOffset = 0.0
    }

    private fun approach(current: Double, target: Double, amount: Double): Double = when {
        current < target -> minOf(current + amount, target)
        current > target -> maxOf(current - amount, target)
        else -> current
    }

    companion object {
        private const val DEFAULT_AMPLITUDE = 0.015
        private const val DEFAULT_CYCLE_DISTANCE = 0.9
        private const val DEFAULT_RESPONSE_PER_SECOND = 8.0
        private const val MOVEMENT_EPSILON = 1e-9
        private const val FULL_TURN = PI * 2.0
    }
}
