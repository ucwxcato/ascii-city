package com.asciicity.render

import com.asciicity.entity.WorldEntity
import com.asciicity.entity.WorldEntityType
import com.asciicity.world.WorldGrid
import com.asciicity.world.WorldPosition
import kotlin.math.abs
import kotlin.math.roundToInt

class StreetLampRenderer(
    entities: Collection<WorldEntity>,
    grid: WorldGrid?,
    private val palette: RenderPalette,
) {
    private val lamps = createLamps(entities, grid)

    constructor(entities: Collection<WorldEntity>, palette: RenderPalette) : this(entities, null, palette)

    internal val lampCount: Int
        get() = lamps.size

    fun render(frame: AsciiFrame, pose: ViewPose, metrics: ProjectionMetrics) {
        var lampIndex = 0
        while (lampIndex < lamps.size) {
            renderLamp(frame, pose, metrics, lamps[lampIndex])
            lampIndex += 1
        }
    }

    private fun renderLamp(
        frame: AsciiFrame,
        pose: ViewPose,
        metrics: ProjectionMetrics,
        lamp: LampFixture,
    ) {
        val relativeX = lamp.position.x - pose.position.x
        val relativeZ = lamp.position.z - pose.position.z
        val depth = relativeX * pose.forwardX + relativeZ * pose.forwardZ
        if (depth < MINIMUM_DEPTH || depth > MAXIMUM_DRAW_DISTANCE) return

        val lateralOffset = relativeX * pose.rightX + relativeZ * pose.rightZ
        val normalizedLateral = lateralOffset / depth
        if (abs(normalizedLateral) > metrics.halfFovTangent * FOV_MARGIN) return

        val column = ((normalizedLateral / metrics.halfFovTangent + 1.0) * frame.width * 0.5).roundToInt()
        if (column < 0 || column >= frame.width) return
        val bulbRow = metrics.projectWorldHeight(lamp.baseHeight + LAMP_HEIGHT, pose.eyeHeight, depth).roundToInt()
        val baseRow = metrics.projectWorldHeight(lamp.baseHeight, pose.eyeHeight, depth).roundToInt()
        val lampDepth = depth.toFloat()

        renderGlow(frame, column, bulbRow, lampDepth, depth, palette[PaletteKeys.NEON_AMBER])
        writeGlyph(frame, column, bulbRow, 'o'.code, palette[PaletteKeys.WINDOW_LIT], lampDepth, BULB_EMISSIVE_STRENGTH)
        writeGlyph(frame, column - 1, bulbRow, '-'.code, palette[PaletteKeys.NEON_AMBER], lampDepth, ARM_EMISSIVE_STRENGTH)
        writeGlyph(frame, column + 1, bulbRow, '-'.code, palette[PaletteKeys.NEON_AMBER], lampDepth, ARM_EMISSIVE_STRENGTH)
        val poleStart = minOf(baseRow, bulbRow)
        val poleEnd = maxOf(baseRow, bulbRow)
        var row = poleStart
        while (row <= poleEnd) {
            writeGlyph(frame, column, row, '|'.code, palette[PaletteKeys.FOG], lampDepth)
            row += 1
        }
    }

    private fun renderGlow(
        frame: AsciiFrame,
        centerColumn: Int,
        centerRow: Int,
        lampDepth: Float,
        depth: Double,
        glowColor: Int,
    ) {
        val radius = (metricsIndependentGlowRadius(depth)).roundToInt().coerceIn(1, MAX_GLOW_RADIUS)
        val radiusSquared = radius * radius
        var row = centerRow - radius
        while (row <= centerRow + radius) {
            if (row >= 0 && row < frame.height) {
                var column = centerColumn - radius
                while (column <= centerColumn + radius) {
                    if (column >= 0 && column < frame.width && frame.depthAt(column, row) > lampDepth) {
                        val horizontal = column - centerColumn
                        val vertical = row - centerRow
                        val distanceSquared = horizontal * horizontal + vertical * vertical
                        if (distanceSquared <= radiusSquared) {
                            val intensity = (1.0 - distanceSquared.toDouble() / (radiusSquared + 1.0)) * GLOW_STRENGTH
                            frame.set(
                                x = column,
                                y = row,
                                glyph = frame.glyphAt(column, row),
                                foreground = blend(frame.foregroundAt(column, row), glowColor, intensity),
                                background = blend(frame.backgroundAt(column, row), glowColor, intensity * BACKGROUND_GLOW_MULTIPLIER),
                                depth = frame.depthAt(column, row),
                                emissive = frame.emissiveAt(column, row),
                            )
                        }
                    }
                    column += 1
                }
            }
            row += 1
        }
    }

    private fun metricsIndependentGlowRadius(depth: Double): Double = GLOW_SIZE_AT_ONE_WORLD_UNIT / depth

    private fun createLamps(entities: Collection<WorldEntity>, grid: WorldGrid?): Array<LampFixture> {
        val lamps = ArrayList<LampFixture>()
        val occupiedCells = if (grid == null) null else BooleanArray(grid.width * grid.depth)
        for (entity in entities) {
            if (entity.type != WorldEntityType.LAMP) continue
            val baseHeight = grid?.terrainHeightAt(entity.position.x, entity.position.z) ?: 0.0
            lamps += LampFixture(entity.position, baseHeight)
            if (grid != null) {
                val cell = entity.position.toGridCell()
                occupiedCells!![cell.z * grid.width + cell.x] = true
            }
        }
        if (grid != null) addRoadsideLamps(grid, lamps, occupiedCells!!)
        return lamps.toTypedArray()
    }

    private fun addRoadsideLamps(
        grid: WorldGrid,
        lamps: MutableList<LampFixture>,
        occupiedCells: BooleanArray,
    ) {
        for (z in 0 until grid.depth) {
            for (x in 0 until grid.width) {
                if (!grid.cellAt(x, z).tags.contains(SIDEWALK_TAG)) continue
                val roadRunsNorthSouth = isRoad(grid, x - 1, z) || isRoad(grid, x + 1, z)
                val roadRunsEastWest = isRoad(grid, x, z - 1) || isRoad(grid, x, z + 1)
                val shouldPlaceLamp =
                    (roadRunsNorthSouth && positiveMod(z - LAMP_GRID_OFFSET, LAMP_GRID_SPACING) == 0) ||
                        roadRunsEastWest && positiveMod(x - LAMP_GRID_OFFSET, LAMP_GRID_SPACING) == 0
                if (!shouldPlaceLamp) continue
                val index = z * grid.width + x
                if (occupiedCells[index]) continue
                occupiedCells[index] = true
                val position = WorldPosition(x + 0.5, z + 0.5)
                lamps += LampFixture(position, grid.terrainHeightAt(position.x, position.z))
            }
        }
    }

    private fun isRoad(grid: WorldGrid, x: Int, z: Int): Boolean =
        grid.contains(x, z) && ROAD_TAG in grid.cellAt(x, z).tags

    private fun positiveMod(value: Int, divisor: Int): Int {
        val result = value % divisor
        return if (result < 0) result + divisor else result
    }

    private fun writeGlyph(
        frame: AsciiFrame,
        column: Int,
        row: Int,
        glyph: Int,
        color: Int,
        depth: Float,
        emissive: Float = 0f,
    ) {
        if (column < 0 || column >= frame.width || row < 0 || row >= frame.height) return
        frame.writeIfNearer(
            x = column,
            y = row,
            glyph = glyph,
            foreground = color,
            background = frame.backgroundAt(column, row),
            depth = depth,
            emissive = emissive,
        )
    }

    private fun blend(from: Int, to: Int, amount: Double): Int {
        val inverse = 1.0 - amount
        val red = (((from ushr 24) and 0xFF) * inverse + ((to ushr 24) and 0xFF) * amount).toInt()
        val green = (((from ushr 16) and 0xFF) * inverse + ((to ushr 16) and 0xFF) * amount).toInt()
        val blue = (((from ushr 8) and 0xFF) * inverse + ((to ushr 8) and 0xFF) * amount).toInt()
        val alpha = (((from and 0xFF) * inverse + (to and 0xFF) * amount).toInt())
        return red shl 24 or (green shl 16) or (blue shl 8) or alpha
    }

    companion object {
        private const val ROAD_TAG = "road"
        private const val SIDEWALK_TAG = "sidewalk"
        private const val LAMP_GRID_SPACING = 8
        private const val LAMP_GRID_OFFSET = 4
        private const val LAMP_HEIGHT = 2.0
        private const val MINIMUM_DEPTH = 0.25
        private const val MAXIMUM_DRAW_DISTANCE = 30.0
        private const val FOV_MARGIN = 1.08
        private const val GLOW_SIZE_AT_ONE_WORLD_UNIT = 18.0
        private const val MAX_GLOW_RADIUS = 3
        private const val GLOW_STRENGTH = 0.32
        private const val BACKGROUND_GLOW_MULTIPLIER = 0.24
        private const val BULB_EMISSIVE_STRENGTH = 1.0f
        private const val ARM_EMISSIVE_STRENGTH = 0.55f
    }

    private data class LampFixture(
        val position: WorldPosition,
        val baseHeight: Double,
    )
}
