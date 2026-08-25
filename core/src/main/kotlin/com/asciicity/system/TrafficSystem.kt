package com.asciicity.system

import com.asciicity.content.AsciiSpriteRegistry
import com.asciicity.render.AsciiFrame
import com.asciicity.render.AsciiSprite
import com.asciicity.render.EntitySpriteRenderer
import com.asciicity.render.ProjectionMetrics
import com.asciicity.render.TrafficLabelRenderer
import com.asciicity.render.ViewPose
import com.asciicity.traffic.TrafficCar

/** Draws the small authored traffic loops without adding collision, input, or AI state. */
class TrafficSystem(
    trafficCars: Collection<TrafficCar>,
    sprites: AsciiSpriteRegistry,
    private val renderer: EntitySpriteRenderer = EntitySpriteRenderer(),
) {
    private val cars = trafficCars.sortedBy(TrafficCar::id).toTypedArray()
    private val carSprites: Array<AsciiSprite>
    private val labelRenderer = TrafficLabelRenderer()

    init {
        require(cars.map(TrafficCar::id).toSet().size == cars.size) { "Traffic car IDs must be unique" }
        carSprites = Array(cars.size) { index ->
            val car = cars[index]
            require(sprites.contains(car.spriteId)) {
                "Traffic car '${car.id}' references unknown sprite '${car.spriteId}'"
            }
            sprites.sprite(car.spriteId)
        }
    }

    val carCount: Int
        get() = cars.size

    fun render(frame: AsciiFrame, pose: ViewPose, metrics: ProjectionMetrics, timeSeconds: Double): Int {
        var writtenCells = 0
        for (index in cars.indices) {
            val car = cars[index]
            val position = car.positionAt(timeSeconds)
            writtenCells += renderer.render(
                frame = frame,
                pose = pose,
                metrics = metrics,
                sprite = carSprites[index],
                position = position,
            )
            writtenCells += labelRenderer.render(frame, pose, metrics, carSprites[index], position, car.label)
        }
        return writtenCells
    }

    internal fun carIdAt(index: Int): String = cars[index].id
}
