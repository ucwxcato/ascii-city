package com.asciicity.system

import com.asciicity.entity.PlayerState
import com.asciicity.input.InputController
import com.asciicity.render.ViewPose
import com.asciicity.world.GridCell
import com.asciicity.world.WorldPosition
import com.badlogic.gdx.Input
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DebugVehicleSystemTest {
    @Test
    fun `vehicle uses WASD car steering and restores the on-foot collider`() {
        val world = SystemTestWorld.create(width = 20, depth = 20, playerPosition = WorldPosition(10.0, 10.0))
        val input = InputController()
        val vehicle = DebugVehicleSystem(CollisionSystem(world), onFootRadius = 0.22, onFootEyeHeight = 0.60)
        val spawned = assertNotNull(vehicle.toggle(world.player))
        assertTrue(vehicle.isDriving)
        assertEquals(DebugVehicleSystem.VEHICLE_RADIUS, spawned.radius, 1e-12)

        input.keyDown(Input.Keys.W)
        input.keyDown(Input.Keys.D)
        val driven = vehicle.update(spawned, input, 0.5)
        assertTrue(driven.position.x > spawned.position.x)
        assertTrue(driven.position.z > spawned.position.z)
        assertTrue(driven.yawRadians > spawned.yawRadians)

        val removed = assertNotNull(vehicle.toggle(driven))
        assertFalse(vehicle.isDriving)
        assertEquals(0.22, removed.radius, 1e-12)
        assertEquals(0.60, removed.eyeHeight, 1e-12)
    }

    @Test
    fun `vehicle cannot spawn without its semantic collision radius and follow camera stays behind it`() {
        val world = SystemTestWorld.create(
            width = 12,
            depth = 12,
            playerPosition = WorldPosition(3.5, 3.7),
            solidCells = setOf(GridCell(3, 4)),
        )
        val vehicle = DebugVehicleSystem(CollisionSystem(world), onFootRadius = 0.22, onFootEyeHeight = 0.60)
        assertEquals(null, vehicle.toggle(world.player))

        val openWorld = SystemTestWorld.create(width = 12, depth = 12, playerPosition = WorldPosition(6.0, 6.0))
        val openVehicle = DebugVehicleSystem(CollisionSystem(openWorld), onFootRadius = 0.22, onFootEyeHeight = 0.60)
        val car = assertNotNull(openVehicle.toggle(openWorld.player))
        val view = openVehicle.thirdPersonPose(
            ViewPose(car.position, car.eyeHeight, car.yawRadians, car.pitchRadians, ViewPose.DEFAULT_FOV_RADIANS),
        )
        assertTrue(view.position.z < car.position.z)
        assertEquals(Math.toRadians(-8.0), view.pitchRadians, 1e-12)
    }
}
