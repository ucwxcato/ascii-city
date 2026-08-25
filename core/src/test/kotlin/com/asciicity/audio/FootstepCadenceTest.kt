package com.asciicity.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class FootstepCadenceTest {
    @Test
    fun `actual movement distance emits alternating footsteps`() {
        val cadence = FootstepCadence(stepDistance = 0.8, firstStepDistance = 0.4)

        assertNull(cadence.update(0.39, FootstepSurface.PAVEMENT))
        assertEquals(AudioCue.FOOTSTEP_PAVEMENT_A, cadence.update(0.02, FootstepSurface.PAVEMENT))
        assertNull(cadence.update(0.77, FootstepSurface.PAVEMENT))
        assertEquals(AudioCue.FOOTSTEP_PAVEMENT_B, cadence.update(0.03, FootstepSurface.PAVEMENT))
        assertEquals(AudioCue.FOOTSTEP_PAVEMENT_A, cadence.update(0.8, FootstepSurface.PAVEMENT))
    }

    @Test
    fun `stationary collision attempts stay silent and reset restores first foot`() {
        val cadence = FootstepCadence(stepDistance = 1.0, firstStepDistance = 0.5)

        assertNull(cadence.update(0.25, FootstepSurface.NATURAL))
        repeat(120) { assertNull(cadence.update(0.0, FootstepSurface.NATURAL)) }
        assertEquals(AudioCue.FOOTSTEP_NATURAL_A, cadence.update(0.25, FootstepSurface.NATURAL))
        assertEquals(AudioCue.FOOTSTEP_NATURAL_B, cadence.update(1.0, FootstepSurface.NATURAL))

        cadence.reset()
        assertEquals(AudioCue.FOOTSTEP_NATURAL_A, cadence.update(0.5, FootstepSurface.NATURAL))
    }

    @Test
    fun `semantic natural tags select park sounds and surface changes preserve alternating feet`() {
        assertEquals(FootstepSurface.NATURAL, FootstepSurface.fromCellTags(setOf("park", "walkable")))
        assertEquals(FootstepSurface.NATURAL, FootstepSurface.fromCellTags(setOf("path", "walkable")))
        assertEquals(FootstepSurface.NATURAL, FootstepSurface.fromCellTags(setOf("grass", "walkable")))
        assertEquals(FootstepSurface.NATURAL, FootstepSurface.fromCellTags(setOf("natural", "walkable")))
        assertEquals(FootstepSurface.PAVEMENT, FootstepSurface.fromCellTags(setOf("road", "walkable")))

        val cadence = FootstepCadence(stepDistance = 1.0, firstStepDistance = 0.5)
        assertEquals(AudioCue.FOOTSTEP_NATURAL_A, cadence.update(0.5, FootstepSurface.NATURAL))
        assertEquals(AudioCue.FOOTSTEP_PAVEMENT_B, cadence.update(1.0, FootstepSurface.PAVEMENT))
    }

    @Test
    fun `invalid cadence values fail early`() {
        assertFailsWith<IllegalArgumentException> { FootstepCadence(stepDistance = 0.0) }
        assertFailsWith<IllegalArgumentException> {
            FootstepCadence(stepDistance = 1.0, firstStepDistance = 1.1)
        }
        val cadence = FootstepCadence()
        assertFailsWith<IllegalArgumentException> { cadence.update(-0.1, FootstepSurface.PAVEMENT) }
        assertFailsWith<IllegalArgumentException> { cadence.update(Double.NaN, FootstepSurface.PAVEMENT) }
    }
}
