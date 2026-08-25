package com.asciicity.input

import com.badlogic.gdx.Input

class PresentationVerificationScript {
    private var lastEventStep = -1
    private var lastMotionPhase = -1

    enum class Event {
        START,
        PAUSE_RESUME,
        MUSIC_ON,
        MUSIC_OFF,
        RESIZE_MINIMUM,
        RESIZE_ULTRAWIDE,
        RESIZE_REFERENCE,
        COMPLETE,
    }

    fun apply(step: Int, input: InputController): Event? {
        updateMotion(step, input)
        if (step == lastEventStep) return null
        val event = when (step) {
            0 -> {
                input.keyDown(Input.Keys.F3)
                input.keyUp(Input.Keys.F3)
                Event.START
            }
            120 -> Event.PAUSE_RESUME
            180 -> {
                press(input, Input.Keys.M)
                Event.MUSIC_ON
            }
            210 -> {
                press(input, Input.Keys.M)
                Event.MUSIC_OFF
            }
            240 -> Event.RESIZE_MINIMUM
            330 -> Event.RESIZE_ULTRAWIDE
            390 -> Event.RESIZE_REFERENCE
            COMPLETE_STEP -> Event.COMPLETE
            else -> null
        }
        if (event != null) lastEventStep = step
        return event
    }

    private fun updateMotion(step: Int, input: InputController) {
        val motionPhase = (step / MOTION_PHASE_STEPS) % 4
        if (motionPhase == lastMotionPhase) return
        lastMotionPhase = motionPhase
        input.clear()
        when (motionPhase) {
            0 -> hold(input, Input.Keys.W, Input.Keys.RIGHT)
            1 -> hold(input, Input.Keys.D, Input.Keys.UP)
            2 -> hold(input, Input.Keys.S, Input.Keys.LEFT)
            else -> hold(input, Input.Keys.A, Input.Keys.DOWN)
        }
    }

    private fun hold(input: InputController, vararg keys: Int) {
        for (key in keys) input.keyDown(key)
    }

    private fun press(input: InputController, key: Int) {
        input.keyDown(key)
        input.keyUp(key)
    }

    companion object {
        const val COMPLETE_STEP = 480
        private const val MOTION_PHASE_STEPS = 60
    }
}
