package com.asciicity.input

import com.badlogic.gdx.Input
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InputControllerTest {
    @Test
    fun `WASD and arrows map to independent held actions`() {
        val input = InputController()
        val mappings = mapOf(
            Input.Keys.W to GameAction.MOVE_FORWARD,
            Input.Keys.S to GameAction.MOVE_BACKWARD,
            Input.Keys.A to GameAction.STRAFE_LEFT,
            Input.Keys.D to GameAction.STRAFE_RIGHT,
            Input.Keys.LEFT to GameAction.LOOK_LEFT,
            Input.Keys.RIGHT to GameAction.LOOK_RIGHT,
            Input.Keys.UP to GameAction.LOOK_UP,
            Input.Keys.DOWN to GameAction.LOOK_DOWN,
        )
        for ((key, action) in mappings) {
            assertTrue(input.keyDown(key))
            assertTrue(input.isHeld(action))
            assertTrue(input.keyUp(key))
            assertFalse(input.isHeld(action))
        }
        assertFalse(input.keyDown(Input.Keys.SPACE))
    }

    @Test
    fun `commands are edge-triggered and clear prevents stuck input`() {
        val input = InputController()
        for (key in listOf(Input.Keys.E, Input.Keys.ENTER, Input.Keys.NUMPAD_ENTER)) {
            input.keyDown(key)
            assertTrue(input.consume(GameCommand.INTERACT))
            assertFalse(input.consume(GameCommand.INTERACT))
        }
        input.keyDown(Input.Keys.ESCAPE)
        input.keyDown(Input.Keys.F3)
        input.keyDown(Input.Keys.TAB)
        input.keyDown(Input.Keys.C)
        input.keyDown(Input.Keys.M)
        assertTrue(input.consume(GameCommand.CANCEL))
        assertTrue(input.consume(GameCommand.TOGGLE_DEBUG))
        assertTrue(input.consume(GameCommand.CYCLE_DEBUG_MAP))
        assertTrue(input.consume(GameCommand.TOGGLE_CRT))
        assertTrue(input.consume(GameCommand.TOGGLE_MUSIC))

        input.keyDown(Input.Keys.W)
        input.gameplayEnabled = false
        assertFalse(input.isHeld(GameAction.MOVE_FORWARD))
        input.keyDown(Input.Keys.W)
        assertFalse(input.isHeld(GameAction.MOVE_FORWARD))
        input.gameplayEnabled = true
        assertFalse(input.isHeld(GameAction.MOVE_FORWARD))
    }

    @Test
    fun `modal input suppresses gameplay but keeps close commands`() {
        val input = InputController()
        input.keyDown(Input.Keys.W)
        input.modalActive = true

        assertFalse(input.isHeld(GameAction.MOVE_FORWARD))
        input.keyDown(Input.Keys.D)
        input.keyDown(Input.Keys.F3)
        input.keyDown(Input.Keys.TAB)
        input.keyDown(Input.Keys.C)
        input.keyDown(Input.Keys.M)
        input.keyDown(Input.Keys.E)
        input.keyDown(Input.Keys.ESCAPE)
        assertFalse(input.isHeld(GameAction.STRAFE_RIGHT))
        assertFalse(input.consume(GameCommand.TOGGLE_DEBUG))
        assertFalse(input.consume(GameCommand.CYCLE_DEBUG_MAP))
        assertFalse(input.consume(GameCommand.TOGGLE_CRT))
        assertTrue(input.consume(GameCommand.TOGGLE_MUSIC))
        assertTrue(input.consume(GameCommand.INTERACT))
        assertTrue(input.consume(GameCommand.CANCEL))

        input.modalActive = false
        assertFalse(input.isHeld(GameAction.MOVE_FORWARD))
        assertFalse(input.isHeld(GameAction.STRAFE_RIGHT))
    }
}
