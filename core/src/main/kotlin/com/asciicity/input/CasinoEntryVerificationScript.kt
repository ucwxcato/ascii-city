package com.asciicity.input

import com.asciicity.world.WorldPosition
import com.badlogic.gdx.Input

class CasinoEntryVerificationScript {
    private var pressed = false

    fun apply(step: Int, input: InputController) {
        if (pressed || step < ENTRY_STEP) return
        input.clear()
        input.keyDown(Input.Keys.E)
        input.keyUp(Input.Keys.E)
        pressed = true
    }

    companion object {
        const val ENTRY_STEP = 5
        val INITIAL_POSITION = WorldPosition(26.5, 5.8)
    }
}
