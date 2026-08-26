package com.asciicity.render

import com.asciicity.content.AsciiSpriteRegistry
import com.asciicity.system.DebugVehicleSystem
import com.asciicity.world.WorldPosition

/** Draws the active debug car without contributing gameplay state or collision. */
class DebugVehicleRenderer(
    sprites: AsciiSpriteRegistry,
    private val renderer: EntitySpriteRenderer = EntitySpriteRenderer(),
) {
    private val carSprite = sprites.sprite(DebugVehicleSystem.SPRITE_ID)

    fun render(
        frame: AsciiFrame,
        pose: ViewPose,
        metrics: ProjectionMetrics,
        vehiclePosition: WorldPosition?,
    ): Int = vehiclePosition?.let { renderer.render(frame, pose, metrics, carSprite, it) } ?: 0
}
