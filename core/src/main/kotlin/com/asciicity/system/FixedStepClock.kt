package com.asciicity.system

import kotlin.math.floor

class FixedStepClock(
    val fixedDeltaSeconds: Double = 1.0 / 60.0,
    private val maximumFrameDeltaSeconds: Double = 0.25,
    private val maximumCatchUpSteps: Int = 8,
) {
    private var accumulatorSeconds = 0.0

    val interpolationAlpha: Double
        get() = accumulatorSeconds / fixedDeltaSeconds

    init {
        require(fixedDeltaSeconds > 0.0 && fixedDeltaSeconds.isFinite()) { "Fixed delta must be positive and finite" }
        require(maximumFrameDeltaSeconds >= fixedDeltaSeconds && maximumFrameDeltaSeconds.isFinite()) {
            "Maximum frame delta must be finite and at least one fixed step"
        }
        require(maximumCatchUpSteps > 0) { "Maximum catch-up steps must be positive" }
    }

    fun addFrame(frameDeltaSeconds: Double): Int {
        require(frameDeltaSeconds >= 0.0 && frameDeltaSeconds.isFinite()) { "Frame delta must be finite and non-negative" }
        accumulatorSeconds += frameDeltaSeconds.coerceAtMost(maximumFrameDeltaSeconds)
        val availableSteps = floor(accumulatorSeconds / fixedDeltaSeconds).toInt()
        val steps = minOf(availableSteps, maximumCatchUpSteps)
        accumulatorSeconds -= steps * fixedDeltaSeconds
        if (availableSteps > maximumCatchUpSteps) accumulatorSeconds %= fixedDeltaSeconds
        return steps
    }

    fun reset() {
        accumulatorSeconds = 0.0
    }
}
