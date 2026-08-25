package com.asciicity.audio

enum class FootstepSurface {
    NATURAL,
    PAVEMENT;

    companion object {
        private val NATURAL_TAGS = setOf("natural", "park", "grass", "path")

        fun fromCellTags(tags: Set<String>): FootstepSurface =
            if (tags.any(NATURAL_TAGS::contains)) NATURAL else PAVEMENT
    }
}

class FootstepCadence(
    private val stepDistance: Double = DEFAULT_STEP_DISTANCE,
    private val firstStepDistance: Double = stepDistance * 0.5,
) {
    private var distanceRemaining = firstStepDistance
    private var firstFoot = true

    init {
        require(stepDistance > 0.0 && stepDistance.isFinite()) {
            "Footstep distance must be positive and finite"
        }
        require(firstStepDistance > 0.0 && firstStepDistance <= stepDistance && firstStepDistance.isFinite()) {
            "First footstep distance must be positive, finite, and no greater than the full step distance"
        }
    }

    fun update(distanceMoved: Double, surface: FootstepSurface): AudioCue? {
        require(distanceMoved >= 0.0 && distanceMoved.isFinite()) {
            "Footstep movement distance must be finite and non-negative"
        }
        if (distanceMoved == 0.0) return null
        distanceRemaining -= distanceMoved
        if (distanceRemaining > 0.0) return null
        while (distanceRemaining <= 0.0) distanceRemaining += stepDistance
        val cue = when (surface) {
            FootstepSurface.NATURAL -> if (firstFoot) {
                AudioCue.FOOTSTEP_NATURAL_A
            } else {
                AudioCue.FOOTSTEP_NATURAL_B
            }
            FootstepSurface.PAVEMENT -> if (firstFoot) {
                AudioCue.FOOTSTEP_PAVEMENT_A
            } else {
                AudioCue.FOOTSTEP_PAVEMENT_B
            }
        }
        firstFoot = !firstFoot
        return cue
    }

    fun reset() {
        distanceRemaining = firstStepDistance
        firstFoot = true
    }

    companion object {
        private const val DEFAULT_STEP_DISTANCE = 0.9
    }
}
