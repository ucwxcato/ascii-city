package com.asciicity.input

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InteractionVerificationScriptTest {
    @Test
    fun `verification route emits every event once in order`() {
        val script = InteractionVerificationScript()
        val input = InputController()
        val events = mutableListOf<InteractionVerificationScript.Event>()

        repeat(196) { step -> script.apply(step, input)?.let(events::add) }

        assertEquals(InteractionVerificationScript.Event.entries.toList(), events)
        assertNull(script.apply(195, input))
    }
}
