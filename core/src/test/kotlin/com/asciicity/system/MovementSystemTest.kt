package com.asciicity.system

import com.asciicity.entity.PlayerState
import com.asciicity.input.InputController
import com.asciicity.world.GridCell
import com.asciicity.world.WorldPosition
import com.badlogic.gdx.Input
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MovementSystemTest {
    @Test
    fun `WASD follows yaw and arrow keys never translate`() {
        assertPosition(Input.Keys.W, yaw = 0.0, expectedX = 5.0, expectedZ = 8.0)
        assertPosition(Input.Keys.S, yaw = 0.0, expectedX = 5.0, expectedZ = 2.0)
        assertPosition(Input.Keys.A, yaw = 0.0, expectedX = 2.0, expectedZ = 5.0)
        assertPosition(Input.Keys.D, yaw = 0.0, expectedX = 8.0, expectedZ = 5.0)
        assertPosition(Input.Keys.W, yaw = Math.PI / 2.0, expectedX = 8.0, expectedZ = 5.0)

        val input = InputController()
        input.keyDown(Input.Keys.LEFT)
        input.keyDown(Input.Keys.UP)
        val start = PlayerState(WorldPosition(5.0, 5.0), 0.0)
        val result = MovementSystem(CollisionSystem(SystemTestWorld.create(playerPosition = start.position)))
            .update(start, input, 1.0)
        assertEquals(start.position, result.position)
    }

    @Test
    fun `diagonal input is normalized and combined look changes movement heading`() {
        val world = SystemTestWorld.create(width = 20, depth = 20, playerPosition = WorldPosition(10.0, 10.0))
        val input = InputController()
        input.keyDown(Input.Keys.W)
        input.keyDown(Input.Keys.D)
        val movement = MovementSystem(CollisionSystem(world))
        val start = world.player
        val diagonal = movement.update(start, input, 1.0)
        assertEquals(3.0, hypot(diagonal.position.x - 10.0, diagonal.position.z - 10.0), 1e-10)

        input.clear()
        input.keyDown(Input.Keys.W)
        input.keyDown(Input.Keys.RIGHT)
        val simulation = PlayerSimulation(world, input)
        simulation.step(0.5)
        assertTrue(simulation.currentState.position.x > 10.0)
        assertTrue(simulation.currentState.position.z > 10.0)
        assertTrue(simulation.currentState.yawRadians > 0.0)
    }

    @Test
    fun `movement slides deterministically and repeated traces match`() {
        val wall = (1..8).mapTo(mutableSetOf()) { GridCell(4, it) }
        val first = runTrace(wall)
        val second = runTrace(wall)
        assertEquals(first.position.x, second.position.x, 1e-12)
        assertEquals(first.position.z, second.position.z, 1e-12)
        assertEquals(first.yawRadians, second.yawRadians, 1e-12)
        assertTrue(first.position.x <= 3.78 + 1e-9)
        assertTrue(first.position.z > 3.0)
    }

    private fun runTrace(wall: Set<GridCell>): PlayerState {
        val world = SystemTestWorld.create(playerPosition = WorldPosition(3.5, 2.5), solidCells = wall)
        val input = InputController()
        input.keyDown(Input.Keys.W)
        input.keyDown(Input.Keys.D)
        val simulation = PlayerSimulation(world, input)
        repeat(120) { simulation.step(1.0 / 60.0) }
        return simulation.currentState
    }

    private fun assertPosition(key: Int, yaw: Double, expectedX: Double, expectedZ: Double) {
        val start = PlayerState(WorldPosition(5.0, 5.0), yaw)
        val world = SystemTestWorld.create(width = 12, depth = 12, playerPosition = start.position)
        val input = InputController()
        input.keyDown(key)
        val result = MovementSystem(CollisionSystem(world)).update(start, input, 1.0)
        assertEquals(expectedX, result.position.x, 1e-10)
        assertEquals(expectedZ, result.position.z, 1e-10)
    }
}
