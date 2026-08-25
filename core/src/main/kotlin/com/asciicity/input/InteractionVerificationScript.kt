package com.asciicity.input

import com.asciicity.world.WorldPosition
import com.badlogic.gdx.Input

class InteractionVerificationScript {
    private var lastEventStep = -1

    enum class Event {
        SHOW_DEBUG,
        OPEN_TERMINAL,
        RESIZE_MINIMUM,
        RESIZE_ULTRAWIDE,
        RESIZE_REFERENCE,
        ADVANCE_TERMINAL,
        CLOSE_WITH_ENTER,
        TURN_AWAY,
        STOP_TURN_AWAY,
        TURN_BACK,
        STOP_TURN_BACK,
        REOPEN_TERMINAL,
        CLOSE_WITH_ESCAPE,
        COMPLETE,
    }

    fun apply(step: Int, input: InputController): Event? {
        if (step == lastEventStep) return null
        val event = when (step) {
            0 -> press(input, Event.SHOW_DEBUG, Input.Keys.F3)
            50 -> press(input, Event.OPEN_TERMINAL, Input.Keys.E)
            55 -> Event.RESIZE_MINIMUM
            67 -> Event.RESIZE_ULTRAWIDE
            79 -> Event.RESIZE_REFERENCE
            85 -> press(input, Event.ADVANCE_TERMINAL, Input.Keys.ENTER)
            90 -> press(input, Event.CLOSE_WITH_ENTER, Input.Keys.ENTER)
            100 -> hold(input, Event.TURN_AWAY, Input.Keys.RIGHT)
            130 -> clear(input, Event.STOP_TURN_AWAY)
            135 -> hold(input, Event.TURN_BACK, Input.Keys.LEFT)
            165 -> clear(input, Event.STOP_TURN_BACK)
            170 -> press(input, Event.REOPEN_TERMINAL, Input.Keys.E)
            185 -> press(input, Event.CLOSE_WITH_ESCAPE, Input.Keys.ESCAPE)
            195 -> Event.COMPLETE
            else -> null
        }
        if (event != null) lastEventStep = step
        return event
    }

    private fun press(input: InputController, event: Event, key: Int): Event {
        input.clear()
        input.keyDown(key)
        input.keyUp(key)
        return event
    }

    private fun hold(input: InputController, event: Event, key: Int): Event {
        input.clear()
        input.keyDown(key)
        return event
    }

    private fun clear(input: InputController, event: Event): Event {
        input.clear()
        return event
    }

    companion object {
        val INITIAL_POSITION = WorldPosition(51.5, 19.2)
    }
}
