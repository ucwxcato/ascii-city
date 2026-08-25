package com.asciicity.audio

import com.asciicity.util.GameLog
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.Disposable

enum class AmbienceZone {
    CITY,
    PARK,
}

enum class AudioCue {
    FOOTSTEP_NATURAL_A,
    FOOTSTEP_NATURAL_B,
    FOOTSTEP_PAVEMENT_A,
    FOOTSTEP_PAVEMENT_B,
    INTERACTION,
}

enum class AmbientAudioCue {
    VENT_HISS,
    ELECTRIC_SPARK,
    DISTANT_HORN,
}

data class AudioPaths(
    val cityAmbience: String = DEFAULT_CITY_AMBIENCE,
    val parkAmbience: String = DEFAULT_PARK_AMBIENCE,
    val rainAmbience: String = DEFAULT_RAIN_AMBIENCE,
    val waterfallAmbience: String = DEFAULT_WATERFALL_AMBIENCE,
    val naturalFootstepA: String = DEFAULT_NATURAL_FOOTSTEP_A,
    val naturalFootstepB: String = DEFAULT_NATURAL_FOOTSTEP_B,
    val pavementFootstepA: String = DEFAULT_PAVEMENT_FOOTSTEP_A,
    val pavementFootstepB: String = DEFAULT_PAVEMENT_FOOTSTEP_B,
    val interactionCue: String? = null,
    val ventHissCue: String = DEFAULT_VENT_HISS,
    val electricSparkCue: String = DEFAULT_ELECTRIC_SPARK,
    val distantHornCue: String = DEFAULT_DISTANT_HORN,
) {
    init {
        require(cityAmbience.isNotBlank()) { "City ambience path must not be blank" }
        require(parkAmbience.isNotBlank()) { "Park ambience path must not be blank" }
        require(rainAmbience.isNotBlank()) { "Rain ambience path must not be blank" }
        require(waterfallAmbience.isNotBlank()) { "Waterfall ambience path must not be blank" }
        require(naturalFootstepA.isNotBlank()) { "First natural footstep path must not be blank" }
        require(naturalFootstepB.isNotBlank()) { "Second natural footstep path must not be blank" }
        require(pavementFootstepA.isNotBlank()) { "First pavement footstep path must not be blank" }
        require(pavementFootstepB.isNotBlank()) { "Second pavement footstep path must not be blank" }
        require(interactionCue == null || interactionCue.isNotBlank()) { "Interaction cue path must not be blank" }
        require(ventHissCue.isNotBlank()) { "Vent hiss cue path must not be blank" }
        require(electricSparkCue.isNotBlank()) { "Electric spark cue path must not be blank" }
        require(distantHornCue.isNotBlank()) { "Distant horn cue path must not be blank" }
    }

    companion object {
        const val DEFAULT_CITY_AMBIENCE = "audio/neon-city-loop.ogg"
        const val DEFAULT_PARK_AMBIENCE = "audio/signal-garden-loop.ogg"
        const val DEFAULT_RAIN_AMBIENCE = "audio/gentle-rain-loop.ogg"
        const val DEFAULT_WATERFALL_AMBIENCE = "audio/waterfall-loop.ogg"
        const val DEFAULT_NATURAL_FOOTSTEP_A = "audio/footstep-natural-a.ogg"
        const val DEFAULT_NATURAL_FOOTSTEP_B = "audio/footstep-natural-b.ogg"
        const val DEFAULT_PAVEMENT_FOOTSTEP_A = "audio/footstep-pavement-a.ogg"
        const val DEFAULT_PAVEMENT_FOOTSTEP_B = "audio/footstep-pavement-b.ogg"
        const val DEFAULT_VENT_HISS = "audio/ambient-vent-hiss.ogg"
        const val DEFAULT_ELECTRIC_SPARK = "audio/ambient-electric-spark.ogg"
        const val DEFAULT_DISTANT_HORN = "audio/ambient-distant-horn.ogg"
    }
}

class AudioService internal constructor(
    private val cityTrack: LoopingTrack?,
    private val parkTrack: LoopingTrack?,
    private val rainTrack: LoopingTrack? = null,
    private val waterfallTrack: LoopingTrack? = null,
    private val effects: Map<AudioCue, OneShotSound> = emptyMap(),
    private val ambientEffects: Map<AmbientAudioCue, OneShotSound> = emptyMap(),
    private val masterVolume: Float = DEFAULT_MASTER_VOLUME,
    private val rainVolume: Float = DEFAULT_RAIN_VOLUME,
    private val waterfallVolume: Float = DEFAULT_WATERFALL_VOLUME,
    private val effectsVolume: Float = DEFAULT_EFFECTS_VOLUME,
    private val crossfadeSeconds: Float = DEFAULT_CROSSFADE_SECONDS,
    private val waterfallFadeSeconds: Float = DEFAULT_WATERFALL_FADE_SECONDS,
    initialMusicMuted: Boolean = false,
) : Disposable {
    private var cityMix = 1f
    private var parkMix = 0f
    private var targetZone = AmbienceZone.CITY
    private var waterfallMix = 0f
    private var started = false
    private var paused = false
    private var disposed = false

    var musicMuted: Boolean = initialMusicMuted
        private set

    val available: Boolean
        get() = cityTrack != null || parkTrack != null || rainTrack != null || waterfallTrack != null ||
            effects.isNotEmpty() || ambientEffects.isNotEmpty()

    val musicAvailable: Boolean
        get() = cityTrack != null || parkTrack != null

    init {
        require(masterVolume in 0f..1f) { "Master ambience volume must be between zero and one" }
        require(rainVolume in 0f..1f) { "Rain ambience volume must be between zero and one" }
        require(waterfallVolume in 0f..1f) { "Waterfall ambience volume must be between zero and one" }
        require(effectsVolume in 0f..1f) { "Effects volume must be between zero and one" }
        require(crossfadeSeconds > 0f && crossfadeSeconds.isFinite()) {
            "Ambience crossfade duration must be positive and finite"
        }
        require(waterfallFadeSeconds > 0f && waterfallFadeSeconds.isFinite()) {
            "Waterfall fade duration must be positive and finite"
        }
    }

    fun start(initialZone: AmbienceZone) {
        if (disposed || started) {
            if (started) resume()
            return
        }
        targetZone = initialZone
        snapMixToTarget()
        forEachTrack { track ->
            track.looping = true
            track.volume = 0f
            track.play()
        }
        started = true
        paused = false
        applyVolumes()
    }

    fun update(zone: AmbienceZone, deltaSeconds: Float, waterfallProximity: Float = 0f) {
        require(deltaSeconds >= 0f && deltaSeconds.isFinite()) {
            "Audio update delta must be finite and non-negative"
        }
        require(waterfallProximity in 0f..1f && waterfallProximity.isFinite()) {
            "Waterfall proximity must be finite and between zero and one"
        }
        if (disposed) return
        if (!started) start(zone)
        targetZone = zone
        val amount = (deltaSeconds / crossfadeSeconds).coerceIn(0f, 1f)
        cityMix = approach(cityMix, cityTarget(), amount)
        parkMix = approach(parkMix, parkTarget(), amount)
        waterfallMix = approach(
            waterfallMix,
            if (waterfallTrack == null) 0f else waterfallProximity,
            (deltaSeconds / waterfallFadeSeconds).coerceIn(0f, 1f),
        )
        applyVolumes()
    }

    fun toggleMusicMuted(): Boolean {
        if (disposed) return musicMuted
        musicMuted = !musicMuted
        applyVolumes()
        return musicMuted
    }

    fun play(cue: AudioCue): Boolean {
        if (disposed || paused) return false
        val effect = effects[cue] ?: return false
        effect.play(effectsVolume, pitch = 1f, pan = 0f)
        return true
    }

    fun playAmbient(cue: AmbientAudioCue, volume: Float, pan: Float, pitch: Float = 1f): Boolean {
        require(volume in 0f..1f && volume.isFinite()) { "Ambient effect volume must be between zero and one" }
        require(pan in -1f..1f && pan.isFinite()) { "Ambient effect pan must be between minus one and one" }
        require(pitch in 0.5f..2f && pitch.isFinite()) { "Ambient effect pitch must be between 0.5 and 2.0" }
        if (disposed || paused || volume == 0f) return false
        val effect = ambientEffects[cue] ?: return false
        effect.play(volume * effectsVolume, pitch, pan)
        return true
    }

    fun pause() {
        if (disposed || !started || paused) return
        forEachTrack(LoopingTrack::pause)
        paused = true
    }

    fun resume() {
        if (disposed || !started || !paused) return
        forEachTrack(LoopingTrack::play)
        paused = false
        applyVolumes()
    }

    override fun dispose() {
        if (disposed) return
        disposed = true
        cityTrack?.dispose()
        parkTrack?.dispose()
        rainTrack?.dispose()
        waterfallTrack?.dispose()
        (effects.values + ambientEffects.values).toSet().forEach(OneShotSound::dispose)
    }

    private fun snapMixToTarget() {
        cityMix = cityTarget()
        parkMix = parkTarget()
    }

    private fun cityTarget(): Float = when {
        cityTrack == null -> 0f
        parkTrack == null -> 1f
        targetZone == AmbienceZone.CITY -> 1f
        else -> 0f
    }

    private fun parkTarget(): Float = when {
        parkTrack == null -> 0f
        cityTrack == null -> 1f
        targetZone == AmbienceZone.PARK -> 1f
        else -> 0f
    }

    private fun applyVolumes() {
        val audibleVolume = if (musicMuted || paused) 0f else masterVolume
        cityTrack?.volume = cityMix * audibleVolume
        parkTrack?.volume = parkMix * audibleVolume
        rainTrack?.volume = if (paused) 0f else rainVolume
        waterfallTrack?.volume = if (paused) 0f else waterfallMix * waterfallVolume
    }

    private fun forEachTrack(action: (LoopingTrack) -> Unit) {
        cityTrack?.let(action)
        parkTrack?.let(action)
        rainTrack?.let(action)
        waterfallTrack?.let(action)
    }

    private fun approach(current: Float, target: Float, amount: Float): Float = when {
        current < target -> minOf(current + amount, target)
        current > target -> maxOf(current - amount, target)
        else -> current
    }

    companion object {
        private const val DEFAULT_MASTER_VOLUME = 0.32f
        private const val DEFAULT_RAIN_VOLUME = 0.30f
        private const val DEFAULT_WATERFALL_VOLUME = 0.50f
        private const val DEFAULT_EFFECTS_VOLUME = 0.55f
        private const val DEFAULT_CROSSFADE_SECONDS = 2.5f
        private const val DEFAULT_WATERFALL_FADE_SECONDS = 0.75f

        fun create(
            enabled: Boolean = true,
            paths: AudioPaths = AudioPaths(),
        ): AudioService {
            if (!enabled) {
                GameLog.info("AudioService", "Audio disabled by launch option")
                return AudioService(null, null, initialMusicMuted = true)
            }
            val cityTrack = loadTrack("city", paths.cityAmbience)
            val parkTrack = loadTrack("park", paths.parkAmbience)
            val rainTrack = loadTrack("rain", paths.rainAmbience)
            val waterfallTrack = loadTrack("waterfall", paths.waterfallAmbience)
            val effects = buildMap {
                loadSound("natural footstep A", paths.naturalFootstepA)?.let { sound ->
                    put(AudioCue.FOOTSTEP_NATURAL_A, sound)
                }
                loadSound("natural footstep B", paths.naturalFootstepB)?.let { sound ->
                    put(AudioCue.FOOTSTEP_NATURAL_B, sound)
                }
                loadSound("pavement footstep A", paths.pavementFootstepA)?.let { sound ->
                    put(AudioCue.FOOTSTEP_PAVEMENT_A, sound)
                }
                loadSound("pavement footstep B", paths.pavementFootstepB)?.let { sound ->
                    put(AudioCue.FOOTSTEP_PAVEMENT_B, sound)
                }
                paths.interactionCue?.let { path ->
                    loadSound("interaction", path)?.let { sound -> put(AudioCue.INTERACTION, sound) }
                }
            }
            val ambientEffects = buildMap {
                loadSound("vent hiss", paths.ventHissCue)?.let { put(AmbientAudioCue.VENT_HISS, it) }
                loadSound("electric spark", paths.electricSparkCue)?.let { put(AmbientAudioCue.ELECTRIC_SPARK, it) }
                loadSound("distant horn", paths.distantHornCue)?.let { put(AmbientAudioCue.DISTANT_HORN, it) }
            }
            if (cityTrack == null && parkTrack == null && rainTrack == null && waterfallTrack == null) {
                GameLog.info("AudioService", "No ambience tracks are available; continuing without audio")
            }
            return AudioService(
                cityTrack,
                parkTrack,
                rainTrack,
                waterfallTrack,
                effects,
                ambientEffects,
                initialMusicMuted = true,
            )
        }

        private fun loadTrack(label: String, path: String): LoopingTrack? = try {
            GdxLoopingTrack(Gdx.audio.newMusic(Gdx.files.internal(path))).also {
                GameLog.info("AudioService", "Loaded $label ambience '$path'")
            }
        } catch (error: Exception) {
            GameLog.info(
                "AudioService",
                "Could not load optional $label ambience '$path': ${error.message ?: error::class.simpleName}",
            )
            null
        }

        private fun loadSound(label: String, path: String): OneShotSound? = try {
            GdxOneShotSound(Gdx.audio.newSound(Gdx.files.internal(path))).also {
                GameLog.info("AudioService", "Loaded $label cue '$path'")
            }
        } catch (error: Exception) {
            GameLog.info(
                "AudioService",
                "Could not load optional $label cue '$path': ${error.message ?: error::class.simpleName}",
            )
            null
        }
    }
}

internal interface LoopingTrack : Disposable {
    var looping: Boolean
    var volume: Float

    fun play()

    fun pause()
}

internal interface OneShotSound : Disposable {
    fun play(volume: Float, pitch: Float, pan: Float)
}

private class GdxLoopingTrack(
    private val music: Music,
) : LoopingTrack {
    override var looping: Boolean
        get() = music.isLooping
        set(value) {
            music.isLooping = value
        }

    override var volume: Float
        get() = music.volume
        set(value) {
            music.volume = value
        }

    override fun play() = music.play()

    override fun pause() = music.pause()

    override fun dispose() = music.dispose()
}

private class GdxOneShotSound(
    private val sound: Sound,
) : OneShotSound {
    override fun play(volume: Float, pitch: Float, pan: Float) {
        sound.play(volume, pitch, pan)
    }

    override fun dispose() = sound.dispose()
}
