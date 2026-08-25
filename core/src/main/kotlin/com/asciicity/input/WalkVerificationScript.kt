package com.asciicity.input

import com.badlogic.gdx.Input

class WalkVerificationScript {
    private var lastEventStep = -1

    enum class Event {
        START_FORWARD,
        LIFECYCLE_RESET,
        COLLIDE_RIGHT,
        RESIZE_MINIMUM,
        RETURN_LEFT,
        RESIZE_REFERENCE,
        RETURN_BACKWARD,
        LOOK_RIGHT_UP,
        LOOK_LEFT_DOWN,
        STOP_PITCH,
        COMPLETE,
    }

    fun apply(step: Int, input: InputController): Event? {
        if (step == lastEventStep) return null
        val event = when (step) {
            0 -> change(input, Event.START_FORWARD, Input.Keys.W)
            120 -> Event.LIFECYCLE_RESET
            180 -> change(input, Event.COLLIDE_RIGHT, Input.Keys.D)
            240 -> Event.RESIZE_MINIMUM
            300 -> change(input, Event.RETURN_LEFT, Input.Keys.A)
            330 -> Event.RESIZE_REFERENCE
            385 -> change(input, Event.RETURN_BACKWARD, Input.Keys.S)
            565 -> change(input, Event.LOOK_RIGHT_UP, Input.Keys.RIGHT, Input.Keys.UP)
            625 -> change(input, Event.LOOK_LEFT_DOWN, Input.Keys.LEFT, Input.Keys.DOWN)
            669 -> release(input, Event.STOP_PITCH, Input.Keys.DOWN)
            685 -> change(input, Event.COMPLETE)
            else -> null
        }
        if (event != null) lastEventStep = step
        return event
    }

    private fun change(
        input: InputController,
        event: Event,
        vararg heldKeys: Int,
    ): Event {
        input.clear()
        for (key in heldKeys) input.keyDown(key)
        return event
    }

    private fun release(input: InputController, event: Event, key: Int): Event {
        input.keyUp(key)
        return event
    }
}
