package com.asciicity.input

import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter

class InputController : InputAdapter() {
    private val held = BooleanArray(GameAction.entries.size)
    private val pending = BooleanArray(GameCommand.entries.size)

    var modalActive: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            clear()
        }

    var gameplayEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) clear()
        }

    fun isHeld(action: GameAction): Boolean = gameplayEnabled && !modalActive && held[action.ordinal]

    fun consume(command: GameCommand): Boolean {
        val index = command.ordinal
        val wasPending = pending[index]
        pending[index] = false
        return wasPending
    }

    fun clear() {
        held.fill(false)
        pending.fill(false)
    }

    override fun keyDown(keycode: Int): Boolean {
        actionFor(keycode)?.let {
            if (gameplayEnabled && !modalActive) held[it.ordinal] = true
            return true
        }
        commandFor(keycode)?.let {
            if (
                gameplayEnabled &&
                (!modalActive || it == GameCommand.INTERACT || it == GameCommand.CANCEL || it == GameCommand.TOGGLE_MUSIC)
            ) {
                pending[it.ordinal] = true
            }
            return true
        }
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        val action = actionFor(keycode) ?: return commandFor(keycode) != null
        held[action.ordinal] = false
        return true
    }

    private fun actionFor(keycode: Int): GameAction? = when (keycode) {
        Input.Keys.W -> GameAction.MOVE_FORWARD
        Input.Keys.S -> GameAction.MOVE_BACKWARD
        Input.Keys.A -> GameAction.STRAFE_LEFT
        Input.Keys.D -> GameAction.STRAFE_RIGHT
        Input.Keys.LEFT -> GameAction.LOOK_LEFT
        Input.Keys.RIGHT -> GameAction.LOOK_RIGHT
        Input.Keys.UP -> GameAction.LOOK_UP
        Input.Keys.DOWN -> GameAction.LOOK_DOWN
        else -> null
    }

    private fun commandFor(keycode: Int): GameCommand? = when (keycode) {
        Input.Keys.E, Input.Keys.ENTER, Input.Keys.NUMPAD_ENTER -> GameCommand.INTERACT
        Input.Keys.ESCAPE -> GameCommand.CANCEL
        Input.Keys.F3 -> GameCommand.TOGGLE_DEBUG
        Input.Keys.TAB -> GameCommand.CYCLE_DEBUG_MAP
        Input.Keys.C -> GameCommand.TOGGLE_CRT
        Input.Keys.M -> GameCommand.TOGGLE_MUSIC
        else -> null
    }
}
