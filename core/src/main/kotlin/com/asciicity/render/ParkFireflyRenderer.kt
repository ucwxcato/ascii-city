package com.asciicity.render

import com.asciicity.entity.WorldEntity
import com.asciicity.entity.WorldEntityType
import com.asciicity.world.WorldGrid
import com.asciicity.world.WorldPosition
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/**
 * Derives a small number of stateless green firefly swarms from authored lamps in park/nature cells.
 * Fireflies are visual only: their finite-depth glyphs never create entities or collision.
 */
internal class ParkFireflyRenderer(
    entities: Collection<WorldEntity>,
    private val grid: WorldGrid,
    private val materialSampler: MaterialSampler,
) {
    private val swarms = createSwarms(entities)

    internal val swarmCount: Int
        get() = swarms.size

    internal fun swarmIds(): List<String> = swarms.map(Swarm::id)

    fun render(frame: AsciiFrame, pose: ViewPose, metrics: ProjectionMetrics, timeSeconds: Double): Int {
        var writes = 0
        for (swarm in swarms) {
            val stablePhase = stableUnit(swarm.id)
            for (fireflyIndex in 0 until FIREFLIES_PER_SWARM) {
                val phase = timeSeconds * FLIGHT_RATE + stablePhase + fireflyIndex.toDouble() / FIREFLIES_PER_SWARM
                val angle = phase * TAU
                val radius = BASE_RADIUS + fireflyIndex * RADIUS_STEP
                val worldX = swarm.position.x + cos(angle) * radius
                val worldZ = swarm.position.z + sin(angle * ORBIT_ASPECT + stablePhase * TAU) * radius
                val worldHeight = BASE_HEIGHT +
                    (sin(angle * BOB_RATE + fireflyIndex) * 0.5 + 0.5) * HEIGHT_RANGE
                writes += renderFirefly(frame, pose, metrics, worldX, worldZ, worldHeight, phase)
            }
        }
        return writes
    }

    private fun renderFirefly(
        frame: AsciiFrame,
        pose: ViewPose,
        metrics: ProjectionMetrics,
        worldX: Double,
        worldZ: Double,
        worldHeight: Double,
        phase: Double,
    ): Int {
        val relativeX = worldX - pose.position.x
        val relativeZ = worldZ - pose.position.z
        val depth = relativeX * pose.forwardX + relativeZ * pose.forwardZ
        if (!depth.isFinite() || depth !in MINIMUM_DEPTH..MAXIMUM_DRAW_DISTANCE) return 0
        val lateral = relativeX * pose.rightX + relativeZ * pose.rightZ
        val column = floor(metrics.projectCameraPlaneX(lateral / depth)).toInt()
        val row = floor(metrics.projectWorldHeight(worldHeight, pose.eyeHeight, depth)).toInt()
        if (column !in 0 until frame.width || row !in 0 until frame.height) return 0

        val glow = sin(phase * TAU * GLOW_RATE) * 0.5 + 0.5
        val foreground = materialSampler.foreground(PaletteKeys.NEON_GREEN, side = null, depth = depth)
        // A firefly needs to read as a point of light rather than ground texture at normal play distance.
        // Keep the pulse in the glyph shape/background instead of adding more insects or a broad bloom pass.
        val glyph = if (glow >= BRIGHT_GLOW_THRESHOLD) '*'.code else 'o'.code
        return if (frame.writeIfNearer(
                x = column,
                y = row,
                glyph = glyph,
                foreground = foreground,
                background = blend(frame.backgroundAt(column, row), foreground, glow * BACKGROUND_GLOW_STRENGTH),
                depth = depth.toFloat(),
                emissive = (FIREFLY_BASE_EMISSIVE + glow * FIREFLY_PULSE_EMISSIVE).toFloat(),
            )
        ) 1 else 0
    }

    private fun createSwarms(entities: Collection<WorldEntity>): Array<Swarm> = entities
        .asSequence()
        .filter { it.type == WorldEntityType.LAMP }
        .filter { hasFireflyHabitat(it.position) }
        .map { lamp -> Swarm(lamp.id.value, lamp.position, swarmPriority(lamp.position)) }
        .sortedWith(compareBy<Swarm>({ it.priority }, { stableHash(it.id) }, { it.id }))
        .take(MAXIMUM_SWARMS)
        .toList()
        .toTypedArray()

    private fun swarmPriority(position: WorldPosition): Int = when {
        hasNearbyTag(position, WATERFALL_TAG, WATERFALL_PRIORITY_RADIUS) -> 0
        !hasNearbyTag(position, NATURE_TAG) -> 1
        else -> 2
    }

    private fun hasFireflyHabitat(position: WorldPosition): Boolean =
        hasNearbyTag(position, GRASS_TAG) || hasNearbyTag(position, NATURE_TAG)

    private fun hasNearbyTag(position: WorldPosition, tag: String, radius: Int = ELIGIBILITY_RADIUS): Boolean {
        val center = position.toGridCell()
        for (z in center.z - radius..center.z + radius) {
            for (x in center.x - radius..center.x + radius) {
                if (grid.contains(x, z) && tag in grid.cellAt(x, z).tags) return true
            }
        }
        return false
    }

    private fun stableUnit(id: String): Double = (stableHash(id).ushr(11)).toDouble() / (1L shl 53).toDouble()

    private fun stableHash(id: String): Long {
        var hash = FNV_OFFSET_BASIS
        for (character in id) hash = (hash xor character.code.toLong()) * FNV_PRIME
        return hash
    }

    private fun blend(from: Int, to: Int, amount: Double): Int {
        val inverse = 1.0 - amount
        val red = (((from ushr 24) and 0xFF) * inverse + ((to ushr 24) and 0xFF) * amount).toInt()
        val green = (((from ushr 16) and 0xFF) * inverse + ((to ushr 16) and 0xFF) * amount).toInt()
        val blue = (((from ushr 8) and 0xFF) * inverse + ((to ushr 8) and 0xFF) * amount).toInt()
        val alpha = (((from and 0xFF) * inverse + (to and 0xFF) * amount).toInt())
        return red shl 24 or (green shl 16) or (blue shl 8) or alpha
    }

    private data class Swarm(
        val id: String,
        val position: WorldPosition,
        val priority: Int,
    )

    private companion object {
        const val GRASS_TAG = "grass"
        const val NATURE_TAG = "nature"
        const val WATERFALL_TAG = "waterfall"
        const val ELIGIBILITY_RADIUS = 1
        const val WATERFALL_PRIORITY_RADIUS = 3
        const val MAXIMUM_SWARMS = 4
        const val FIREFLIES_PER_SWARM = 3
        const val MINIMUM_DEPTH = 1e-6
        const val MAXIMUM_DRAW_DISTANCE = 28.0
        const val FLIGHT_RATE = 0.17
        const val GLOW_RATE = 0.78
        const val BASE_RADIUS = 0.28
        const val RADIUS_STEP = 0.14
        const val ORBIT_ASPECT = 0.73
        const val BASE_HEIGHT = 0.55
        const val HEIGHT_RANGE = 0.8
        const val BOB_RATE = 1.7
        const val BRIGHT_GLOW_THRESHOLD = 0.56
        const val BACKGROUND_GLOW_STRENGTH = 0.42
        const val FIREFLY_BASE_EMISSIVE = 0.45
        const val FIREFLY_PULSE_EMISSIVE = 0.50
        const val TAU = Math.PI * 2.0
        const val FNV_OFFSET_BASIS = -3750763034362895579L
        const val FNV_PRIME = 1099511628211L
    }
}
