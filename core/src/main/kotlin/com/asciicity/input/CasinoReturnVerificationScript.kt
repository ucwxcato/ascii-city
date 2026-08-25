package com.asciicity.input

import com.asciicity.world.WorldPosition
import com.badlogic.gdx.Input

class CasinoReturnVerificationScript {
    private var pressed = false

    fun apply(step: Int, input: InputController) {
        if (pressed || step < RETURN_STEP) return
        input.clear()
        input.keyDown(Input.Keys.E)
        input.keyUp(Input.Keys.E)
        pressed = true
    }

    companion object {
        const val RETURN_STEP = 5
        val INITIAL_POSITION = WorldPosition(19.5, 2.5)
    }
}
