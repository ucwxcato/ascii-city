package com.asciicity.system

import com.asciicity.input.InputController
import com.asciicity.world.WorldPosition
import com.badlogic.gdx.Input
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FixedStepClockTest {
    @Test
    fun `different frame sequences produce equivalent authoritative poses`() {
        val sixtyFps = simulate(List(60) { 1.0 / 60.0 })
        val thirtyFps = simulate(List(30) { 1.0 / 30.0 })
        assertEquals(sixtyFps.position.x, thirtyFps.position.x, 1e-10)
        assertEquals(sixtyFps.position.z, thirtyFps.position.z, 1e-10)
        assertEquals(sixtyFps.yawRadians, thirtyFps.yawRadians, 1e-10)
        assertEquals(sixtyFps.pitchRadians, thirtyFps.pitchRadians, 1e-10)
    }

    @Test
    fun `large frame delta is clamped capped and reset cannot burst`() {
        val clock = FixedStepClock()
        assertEquals(8, clock.addFrame(10.0))
        assertTrue(clock.interpolationAlpha in 0.0..<1.0)
        clock.reset()
        assertEquals(0, clock.addFrame(0.0))
        assertEquals(0.0, clock.interpolationAlpha)
    }

    private fun simulate(frames: List<Double>): com.asciicity.entity.PlayerState {
        val world = SystemTestWorld.create(width = 20, depth = 20, playerPosition = WorldPosition(10.0, 10.0))
        val input = InputController()
        input.keyDown(Input.Keys.W)
        input.keyDown(Input.Keys.D)
        input.keyDown(Input.Keys.RIGHT)
        input.keyDown(Input.Keys.UP)
        val simulation = PlayerSimulation(world, input)
        val clock = FixedStepClock()
        for (frame in frames) repeat(clock.addFrame(frame)) { simulation.step(clock.fixedDeltaSeconds) }
        return simulation.currentState
    }
}
