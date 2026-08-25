package com.asciicity.ambient

import com.asciicity.audio.AmbientAudioCue
import com.asciicity.world.WorldPosition

enum class AmbientEventKind(
    val audioCue: AmbientAudioCue,
    val glyphs: String,
) {
    VENT_HISS(AmbientAudioCue.VENT_HISS, ".'"),
    ELECTRIC_SPARK(AmbientAudioCue.ELECTRIC_SPARK, "*+"),
    DISTANT_HORN(AmbientAudioCue.DISTANT_HORN, ")}"),
}

data class AmbientEventEmitter(
    val id: String,
    val kind: AmbientEventKind,
    val position: WorldPosition,
    val height: Double,
    val minimumIntervalSeconds: Double,
    val maximumIntervalSeconds: Double,
    val durationSeconds: Double,
    val audibleRange: Double,
) {
    init {
        require(id.isNotBlank()) { "Ambient event emitter ID must not be blank" }
        require(height >= 0.0 && height <= MAXIMUM_HEIGHT && height.isFinite()) {
            "Ambient event height must be finite and between zero and $MAXIMUM_HEIGHT"
        }
        require(minimumIntervalSeconds in MINIMUM_INTERVAL..MAXIMUM_INTERVAL && minimumIntervalSeconds.isFinite()) {
            "Minimum ambient event interval must be between $MINIMUM_INTERVAL and $MAXIMUM_INTERVAL seconds"
        }
        require(maximumIntervalSeconds in minimumIntervalSeconds..MAXIMUM_INTERVAL && maximumIntervalSeconds.isFinite()) {
            "Maximum ambient event interval must be at least the minimum and at most $MAXIMUM_INTERVAL seconds"
        }
        require(durationSeconds in MINIMUM_DURATION..MAXIMUM_DURATION && durationSeconds.isFinite()) {
            "Ambient event duration must be between $MINIMUM_DURATION and $MAXIMUM_DURATION seconds"
        }
        require(audibleRange in MINIMUM_RANGE..MAXIMUM_RANGE && audibleRange.isFinite()) {
            "Ambient event range must be between $MINIMUM_RANGE and $MAXIMUM_RANGE cells"
        }
    }

    companion object {
        const val MAXIMUM_EMITTERS = 12
        const val MAXIMUM_HEIGHT = 12.0
        const val MINIMUM_INTERVAL = 2.0
        const val MAXIMUM_INTERVAL = 60.0
        const val MINIMUM_DURATION = 0.1
        const val MAXIMUM_DURATION = 2.0
        const val MINIMUM_RANGE = 2.0
        const val MAXIMUM_RANGE = 32.0
    }
}
