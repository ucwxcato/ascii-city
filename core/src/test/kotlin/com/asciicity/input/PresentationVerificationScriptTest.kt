package com.asciicity.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PresentationVerificationScriptTest {
    @Test
    fun `presentation smoke milestones emit once in order`() {
        val script = PresentationVerificationScript()
        val input = InputController()
        val events = mutableListOf<PresentationVerificationScript.Event>()

        repeat(PresentationVerificationScript.COMPLETE_STEP + 1) { step ->
            script.apply(step, input)?.let(events::add)
            if (step == 180 || step == 210) {
                assertTrue(input.consume(GameCommand.TOGGLE_MUSIC))
            }
        }

        assertEquals(PresentationVerificationScript.Event.entries.toList(), events)
        assertNull(script.apply(PresentationVerificationScript.COMPLETE_STEP, input))
    }
}
