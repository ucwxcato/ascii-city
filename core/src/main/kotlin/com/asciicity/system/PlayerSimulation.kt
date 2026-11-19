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
    var previousState: PlayerState = initialState
        private set
    var currentState: PlayerState = initialState
        private set

    init {
        world.updatePlayer(initialState)
    }

    fun step(deltaSeconds: Double) {
        previousState = currentState
        val looked = lookSystem.update(currentState, input, deltaSeconds)
        currentState = movementSystem.update(looked, input, deltaSeconds)
        world.updatePlayer(currentState)
        world.advanceTime(deltaSeconds)
    }

    fun interpolatedPose(alpha: Double): ViewPose = ViewPose.interpolate(
        pose(previousState),
        pose(currentState),
        alpha.coerceIn(0.0, 1.0),
    )

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
