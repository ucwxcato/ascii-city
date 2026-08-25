package com.asciicity.render

import com.asciicity.world.WorldPosition

enum class AtmosphericEmitterKind {
    STEAM,
    ROOFTOP_EXHAUST,
}

data class AtmosphericEmitter(
    val id: String,
    val kind: AtmosphericEmitterKind,
    val position: WorldPosition,
    val baseHeight: Double,
    val riseHeight: Double,
    val drift: Double,
    val periodSeconds: Double,
    val phaseOffset: Double,
) {
    init {
        require(id.isNotBlank()) { "Atmospheric emitter ID must not be blank" }
        require(baseHeight >= 0.0 && baseHeight.isFinite()) { "Emitter base height must be finite and non-negative" }
        require(riseHeight in MINIMUM_RISE..MAXIMUM_RISE && riseHeight.isFinite()) {
            "Emitter rise height must be between $MINIMUM_RISE and $MAXIMUM_RISE"
        }
        require(drift in 0.0..MAXIMUM_DRIFT && drift.isFinite()) {
            "Emitter drift must be between zero and $MAXIMUM_DRIFT"
        }
        require(periodSeconds in MINIMUM_PERIOD..MAXIMUM_PERIOD && periodSeconds.isFinite()) {
            "Emitter period must be between $MINIMUM_PERIOD and $MAXIMUM_PERIOD seconds"
        }
        require(phaseOffset >= 0.0 && phaseOffset < 1.0 && phaseOffset.isFinite()) {
            "Emitter phase offset must be finite, at least zero, and less than one"
        }
    }

    companion object {
        const val MAXIMUM_EMITTERS = 8
        const val MINIMUM_RISE = 0.5
        const val MAXIMUM_RISE = 3.0
        const val MAXIMUM_DRIFT = 0.75
        const val MINIMUM_PERIOD = 1.5
        const val MAXIMUM_PERIOD = 8.0
    }
}
