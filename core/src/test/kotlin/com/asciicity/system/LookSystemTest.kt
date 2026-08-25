package com.asciicity.system

import com.asciicity.entity.PlayerState
import com.asciicity.input.InputController
import com.asciicity.render.ViewPose
import com.asciicity.world.WorldPosition
import com.badlogic.gdx.Input
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals

class LookSystemTest {
    @Test
    fun `simultaneous arrows turn and pitch without translating`() {
        val input = InputController()
        input.keyDown(Input.Keys.RIGHT)
        input.keyDown(Input.Keys.UP)
        val start = player()

        val result = LookSystem().update(start, input, 0.5)

        assertEquals(start.position, result.position)
        assertEquals(Math.toRadians(50.0), result.yawRadians, 1e-12)
        assertEquals(Math.toRadians(37.5), result.pitchRadians, 1e-12)
    }

    @Test
    fun `yaw wraps both ways pitch clamps exactly and clearing stops look`() {
        val system = LookSystem()
        val input = InputController()
        input.keyDown(Input.Keys.LEFT)
        val left = system.update(player(yaw = 0.01), input, 1.0)
        assertEquals(ViewPose.normalizeYaw(0.01 - Math.toRadians(100.0)), left.yawRadians, 1e-12)

        input.clear()
        input.keyDown(Input.Keys.RIGHT)
        input.keyDown(Input.Keys.UP)
        val upper = system.update(player(yaw = PI * 2.0 - 0.01), input, 10.0)
        assertEquals(ViewPose.MAX_PITCH_RADIANS, upper.pitchRadians, 1e-12)
        input.keyUp(Input.Keys.UP)
        input.keyDown(Input.Keys.DOWN)
        val lower = system.update(upper, input, 20.0)
        assertEquals(-ViewPose.MAX_PITCH_RADIANS, lower.pitchRadians, 1e-12)

        input.clear()
        assertEquals(lower, system.update(lower, input, 1.0))
    }

    @Test
    fun `fixed look steps equal one aggregate step`() {
        val input = InputController()
        input.keyDown(Input.Keys.RIGHT)
        input.keyDown(Input.Keys.UP)
        val system = LookSystem()
        var stepped = player()
        repeat(60) { stepped = system.update(stepped, input, 1.0 / 60.0) }
        val aggregate = system.update(player(), input, 1.0)
        assertEquals(aggregate.yawRadians, stepped.yawRadians, 1e-11)
        assertEquals(aggregate.pitchRadians, stepped.pitchRadians, 1e-11)
    }

    private fun player(yaw: Double = 0.0) = PlayerState(WorldPosition(2.5, 2.5), yaw)
}
