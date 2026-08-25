package com.asciicity.input

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CasinoEntryVerificationScriptTest {
    @Test
    fun `entry route presses interact once after the doorway target is available`() {
        val script = CasinoEntryVerificationScript()
        val input = InputController()

        script.apply(CasinoEntryVerificationScript.ENTRY_STEP - 1, input)
        assertFalse(input.consume(GameCommand.INTERACT))

        script.apply(CasinoEntryVerificationScript.ENTRY_STEP, input)
        assertTrue(input.consume(GameCommand.INTERACT))

        script.apply(CasinoEntryVerificationScript.ENTRY_STEP + 1, input)
        assertFalse(input.consume(GameCommand.INTERACT))
    }
}
