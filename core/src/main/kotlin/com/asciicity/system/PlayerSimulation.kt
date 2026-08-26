package com.asciicity.system

import com.asciicity.entity.PlayerState
import com.asciicity.input.InputController
import com.asciicity.render.ViewPose
import com.asciicity.world.GameWorld

class PlayerSimulation(
    private val world: GameWorld,
    private val input: InputController,
    private val lookSystem: LookSystem = LookSystem(),
    private val collisionSystem: CollisionSystem = CollisionSystem(world),
    private val movementSystem: MovementSystem = MovementSystem(collisionSystem),
    initialState: PlayerState = world.player,
) {
    private val debugVehicleSystem = DebugVehicleSystem(
        collision = collisionSystem,
        onFootRadius = initialState.radius,
        onFootEyeHeight = initialState.eyeHeight,
    )

    var previousState: PlayerState = initialState
        private set
    var currentState: PlayerState = initialState
        private set

    val isDrivingDebugVehicle: Boolean
        get() = debugVehicleSystem.isDriving

    init {
        world.updatePlayer(initialState)
    }

    fun step(deltaSeconds: Double) {
        previousState = currentState
        currentState = if (debugVehicleSystem.isDriving) {
            debugVehicleSystem.update(currentState, input, deltaSeconds)
        } else {
            val looked = lookSystem.update(currentState, input, deltaSeconds)
            movementSystem.update(looked, input, deltaSeconds)
        }
        world.updatePlayer(currentState)
        world.advanceTime(deltaSeconds)
    }

    fun toggleDebugVehicle(): Boolean {
        val next = debugVehicleSystem.toggle(currentState) ?: return false
        previousState = currentState
        currentState = next
        world.updatePlayer(next)
        return true
    }

    fun interpolatedPose(alpha: Double): ViewPose = ViewPose.interpolate(
        pose(previousState),
        pose(currentState),
        alpha.coerceIn(0.0, 1.0),
    )

    fun interpolatedViewPose(alpha: Double): ViewPose {
        val vehiclePose = interpolatedPose(alpha)
        return if (debugVehicleSystem.isDriving) debugVehicleSystem.thirdPersonPose(vehiclePose) else vehiclePose
    }

    fun synchronize() {
        previousState = world.player
        currentState = world.player
    }

    private fun pose(player: PlayerState): ViewPose = ViewPose(
        position = player.position,
        eyeHeight = player.eyeHeight,
        yawRadians = ViewPose.normalizeYaw(player.yawRadians),
        pitchRadians = player.pitchRadians.coerceIn(-ViewPose.MAX_PITCH_RADIANS, ViewPose.MAX_PITCH_RADIANS),
        horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
    )
}
