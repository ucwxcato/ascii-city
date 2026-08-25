package com.asciicity.input

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CasinoReturnVerificationScriptTest {
    @Test
    fun `return route presses interact once from the casino entry`() {
        val script = CasinoReturnVerificationScript()
        val input = InputController()

        script.apply(CasinoReturnVerificationScript.RETURN_STEP - 1, input)
        assertFalse(input.consume(GameCommand.INTERACT))

        script.apply(CasinoReturnVerificationScript.RETURN_STEP, input)
        assertTrue(input.consume(GameCommand.INTERACT))
    }
}
