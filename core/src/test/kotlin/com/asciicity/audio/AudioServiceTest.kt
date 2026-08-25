package com.asciicity.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AudioServiceTest {
    @Test
    fun `city and park ambience crossfade without restarting either loop`() {
        val city = FakeLoopingTrack()
        val park = FakeLoopingTrack()
        val rain = FakeLoopingTrack()
        val audio = AudioService(
            city,
            park,
            rainTrack = rain,
            masterVolume = 0.5f,
            rainVolume = 0.2f,
            crossfadeSeconds = 2f,
        )

        audio.start(AmbienceZone.CITY)

        assertTrue(city.looping)
        assertTrue(park.looping)
        assertTrue(rain.looping)
        assertEquals(1, city.playCount)
        assertEquals(1, park.playCount)
        assertEquals(1, rain.playCount)
        assertEquals(0.5f, city.volume)
        assertEquals(0f, park.volume)
        assertEquals(0.2f, rain.volume)

        audio.update(AmbienceZone.PARK, 1f)
        assertEquals(0.25f, city.volume)
        assertEquals(0.25f, park.volume)
        assertEquals(0.2f, rain.volume)

        audio.update(AmbienceZone.PARK, 1f)
        audio.update(AmbienceZone.PARK, 1f)
        assertEquals(0f, city.volume)
        assertEquals(0.5f, park.volume)
        assertEquals(1, city.playCount)
        assertEquals(1, park.playCount)
        assertEquals(1, rain.playCount)
    }

    @Test
    fun `music mute preserves rain and footsteps then restores the current mix`() {
        val city = FakeLoopingTrack()
        val park = FakeLoopingTrack()
        val rain = FakeLoopingTrack()
        val footstep = FakeOneShotSound()
        val audio = AudioService(
            city,
            park,
            rainTrack = rain,
            effects = mapOf(AudioCue.FOOTSTEP_NATURAL_A to footstep),
            masterVolume = 0.4f,
            rainVolume = 0.25f,
            effectsVolume = 0.6f,
            crossfadeSeconds = 1f,
        )
        audio.start(AmbienceZone.PARK)

        assertTrue(audio.musicAvailable)
        assertTrue(audio.toggleMusicMuted())
        assertEquals(0f, city.volume)
        assertEquals(0f, park.volume)
        assertEquals(0.25f, rain.volume)
        assertTrue(audio.play(AudioCue.FOOTSTEP_NATURAL_A))
        assertEquals(listOf(0.6f), footstep.playedVolumes)

        assertFalse(audio.toggleMusicMuted())
        assertEquals(0f, city.volume)
        assertEquals(0.4f, park.volume)
        assertEquals(0.25f, rain.volume)

        audio.pause()
        audio.pause()
        assertEquals(1, city.pauseCount)
        assertEquals(1, park.pauseCount)
        assertEquals(1, rain.pauseCount)
        audio.resume()
        audio.resume()
        assertEquals(2, city.playCount)
        assertEquals(2, park.playCount)
        assertEquals(2, rain.playCount)
    }

    @Test
    fun `one available track remains audible in every zone and disposal is idempotent`() {
        val city = FakeLoopingTrack()
        val audio = AudioService(city, null, masterVolume = 0.3f, crossfadeSeconds = 1f)

        audio.start(AmbienceZone.CITY)
        audio.update(AmbienceZone.PARK, 1f)

        assertTrue(audio.available)
        assertEquals(0.3f, city.volume)
        audio.dispose()
        audio.dispose()
        assertEquals(1, city.disposeCount)
    }

    @Test
    fun `rain ambience remains independent of semantic zone crossfades`() {
        val rain = FakeLoopingTrack()
        val audio = AudioService(
            cityTrack = null,
            parkTrack = null,
            rainTrack = rain,
            rainVolume = 0.35f,
        )

        audio.start(AmbienceZone.CITY)
        audio.update(AmbienceZone.PARK, 10f)

        assertTrue(audio.available)
        assertEquals(0.35f, rain.volume)
        assertEquals(1, rain.playCount)
        audio.dispose()
        audio.dispose()
        assertEquals(1, rain.disposeCount)
    }

    @Test
    fun `waterfall ambience follows proximity while ignoring music mute and respecting lifecycle`() {
        val waterfall = FakeLoopingTrack()
        val audio = AudioService(
            cityTrack = null,
            parkTrack = null,
            waterfallTrack = waterfall,
            waterfallVolume = 0.5f,
            waterfallFadeSeconds = 0.75f,
        )

        audio.start(AmbienceZone.PARK)
        assertTrue(waterfall.looping)
        assertEquals(1, waterfall.playCount)
        assertEquals(0f, waterfall.volume)

        audio.update(AmbienceZone.PARK, 0.375f, waterfallProximity = 1f)
        assertEquals(0.25f, waterfall.volume)
        audio.toggleMusicMuted()
        assertEquals(0.25f, waterfall.volume)
        audio.update(AmbienceZone.PARK, 0.375f, waterfallProximity = 1f)
        assertEquals(0.5f, waterfall.volume)
        assertEquals(1, waterfall.playCount)

        audio.pause()
        assertEquals(1, waterfall.pauseCount)
        audio.resume()
        assertEquals(0.5f, waterfall.volume)
        assertEquals(2, waterfall.playCount)

        audio.update(AmbienceZone.PARK, 0.75f, waterfallProximity = 0f)
        assertEquals(0f, waterfall.volume)
        audio.dispose()
        audio.dispose()
        assertEquals(1, waterfall.disposeCount)
    }

    @Test
    fun `default rain mix stays softer than the active soundtrack`() {
        val city = FakeLoopingTrack()
        val rain = FakeLoopingTrack()
        val audio = AudioService(cityTrack = city, parkTrack = null, rainTrack = rain)

        audio.start(AmbienceZone.CITY)

        assertEquals(0.32f, city.volume)
        assertEquals(0.30f, rain.volume)
    }

    @Test
    fun `initial music mute leaves environmental audio audible`() {
        val city = FakeLoopingTrack()
        val rain = FakeLoopingTrack()
        val audio = AudioService(
            cityTrack = city,
            parkTrack = null,
            rainTrack = rain,
            initialMusicMuted = true,
        )

        audio.start(AmbienceZone.CITY)

        assertTrue(audio.musicMuted)
        assertEquals(0f, city.volume)
        assertEquals(0.30f, rain.volume)
        assertFalse(audio.toggleMusicMuted())
        assertEquals(0.32f, city.volume)
    }

    @Test
    fun `effects channel ignores music mute but respects lifecycle and ownership`() {
        val effect = FakeOneShotSound()
        val audio = AudioService(
            cityTrack = null,
            parkTrack = null,
            effects = mapOf(AudioCue.INTERACTION to effect),
            effectsVolume = 0.6f,
        )

        audio.start(AmbienceZone.CITY)
        assertTrue(audio.available)
        assertFalse(audio.musicAvailable)
        assertTrue(audio.play(AudioCue.INTERACTION))
        assertEquals(listOf(0.6f), effect.playedVolumes)
        assertFalse(audio.play(AudioCue.FOOTSTEP_PAVEMENT_A))

        audio.toggleMusicMuted()
        assertTrue(audio.play(AudioCue.INTERACTION))
        audio.pause()
        assertFalse(audio.play(AudioCue.INTERACTION))

        audio.dispose()
        audio.dispose()
        assertEquals(1, effect.disposeCount)
    }

    @Test
    fun `ambient effects preserve spatial volume pan and pitch`() {
        val effect = FakeOneShotSound()
        val audio = AudioService(
            cityTrack = null,
            parkTrack = null,
            ambientEffects = mapOf(AmbientAudioCue.ELECTRIC_SPARK to effect),
            effectsVolume = 0.5f,
        )

        audio.start(AmbienceZone.CITY)
        assertTrue(audio.playAmbient(AmbientAudioCue.ELECTRIC_SPARK, volume = 0.8f, pan = -0.6f, pitch = 1.1f))
        assertEquals(listOf(0.4f), effect.playedVolumes)
        assertEquals(listOf(-0.6f), effect.playedPans)
        assertEquals(listOf(1.1f), effect.playedPitches)
        assertFalse(audio.playAmbient(AmbientAudioCue.VENT_HISS, volume = 1f, pan = 0f))
        assertFailsWith<IllegalArgumentException> {
            audio.playAmbient(AmbientAudioCue.ELECTRIC_SPARK, volume = 1.1f, pan = 0f)
        }
    }

    @Test
    fun `no audio backend is safe and update rejects invalid time`() {
        val audio = AudioService(null, null)

        assertFalse(audio.available)
        audio.start(AmbienceZone.CITY)
        audio.update(AmbienceZone.PARK, 0.1f)
        assertTrue(audio.toggleMusicMuted())
        audio.pause()
        audio.resume()
        audio.dispose()
        audio.dispose()

        val active = AudioService(FakeLoopingTrack(), null)
        assertFailsWith<IllegalArgumentException> { active.update(AmbienceZone.CITY, Float.NaN) }
        assertFailsWith<IllegalArgumentException> { active.update(AmbienceZone.CITY, -0.1f) }
        assertFailsWith<IllegalArgumentException> {
            active.update(AmbienceZone.CITY, 0.1f, waterfallProximity = Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            active.update(AmbienceZone.CITY, 0.1f, waterfallProximity = 1.1f)
        }
        assertFailsWith<IllegalArgumentException> { AudioService(null, null, rainVolume = 1.1f) }
        assertFailsWith<IllegalArgumentException> { AudioService(null, null, waterfallVolume = 1.1f) }
        assertFailsWith<IllegalArgumentException> { AudioPaths(rainAmbience = "") }
        assertFailsWith<IllegalArgumentException> { AudioPaths(waterfallAmbience = "") }
        assertFailsWith<IllegalArgumentException> { AudioPaths(naturalFootstepA = "") }
        assertFailsWith<IllegalArgumentException> { AudioPaths(pavementFootstepB = "") }
    }

    private class FakeLoopingTrack : LoopingTrack {
        override var looping = false
        override var volume = 0f
        var playCount = 0
        var pauseCount = 0
        var disposeCount = 0

        override fun play() {
            playCount += 1
        }

        override fun pause() {
            pauseCount += 1
        }

        override fun dispose() {
            disposeCount += 1
        }
    }

    private class FakeOneShotSound : OneShotSound {
        val playedVolumes = mutableListOf<Float>()
        val playedPitches = mutableListOf<Float>()
        val playedPans = mutableListOf<Float>()
        var disposeCount = 0

        override fun play(volume: Float, pitch: Float, pan: Float) {
            playedVolumes += volume
            playedPitches += pitch
            playedPans += pan
        }

        override fun dispose() {
            disposeCount += 1
        }
    }
}
