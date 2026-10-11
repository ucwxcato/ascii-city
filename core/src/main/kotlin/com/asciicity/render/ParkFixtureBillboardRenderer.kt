package com.asciicity.render

import com.asciicity.content.MaterialRegistry
import com.asciicity.world.CellDefinition
import com.asciicity.world.WorldGrid
import com.asciicity.world.WorldPosition
import kotlin.math.ceil
import kotlin.math.floor

/** Draws semantic fixture cells as camera-facing silhouettes without changing their collision geometry. */
internal class ParkFixtureBillboardRenderer(
    grid: WorldGrid,
    private val materials: MaterialRegistry,
    private val materialSampler: MaterialSampler,
    private val maxDrawDistance: Double,
) {
    private val fixtures = buildFixtures(grid)

    fun render(frame: AsciiFrame, pose: ViewPose, metrics: ProjectionMetrics, timeSeconds: Double) {
        var fixtureIndex = 0
        while (fixtureIndex < fixtures.size) {
            renderFixture(frame, pose, metrics, timeSeconds, fixtures[fixtureIndex])
            fixtureIndex += 1
        }
    }

    private fun renderFixture(
        frame: AsciiFrame,
        pose: ViewPose,
        metrics: ProjectionMetrics,
        timeSeconds: Double,
        fixture: Fixture,
    ) {
        val relativeX = fixture.position.x - pose.position.x
        val relativeZ = fixture.position.z - pose.position.z
        val depth = relativeX * pose.forwardX + relativeZ * pose.forwardZ
        if (!depth.isFinite() || depth <= MINIMUM_DEPTH || depth > maxDrawDistance) return

        val lateralCenter = relativeX * pose.rightX + relativeZ * pose.rightZ
        if (!lateralCenter.isFinite()) return
        val leftLateral = lateralCenter - BILLBOARD_WIDTH * 0.5
        val rightLateral = lateralCenter + BILLBOARD_WIDTH * 0.5
        val leftScreen = metrics.projectCameraPlaneX(leftLateral / depth)
        val rightScreen = metrics.projectCameraPlaneX(rightLateral / depth)
        if (rightScreen <= 0.0 || leftScreen >= frame.width.toDouble()) return

        val topScreen = metrics.projectWorldHeight(fixture.baseHeight + fixture.definition.height, pose.eyeHeight, depth)
        val bottomScreen = metrics.projectWorldHeight(fixture.baseHeight, pose.eyeHeight, depth)
        if (topScreen <= 0.0 || bottomScreen >= frame.height.toDouble()) return

        val firstColumn = floor(leftScreen).toInt().coerceAtLeast(0)
        val lastColumn = (ceil(rightScreen).toInt() - 1).coerceAtMost(frame.width - 1)
        val firstRow = floor(bottomScreen).toInt().coerceAtLeast(0)
        val lastRow = (ceil(topScreen).toInt() - 1).coerceAtMost(frame.height - 1)
        if (firstColumn > lastColumn || firstRow > lastRow) return

        val material = materials.material(fixture.definition.facadeMaterialId)
        val fixtureBackground = if (SLOT_MACHINE_TAG in fixture.definition.tags) {
            materialSampler.background(material, side = null, depth = depth)
        } else {
            null
        }
        val fixtureBackgroundWeight = slotMachineBackgroundWeight(depth)
        val fixtureDepth = depth.toFloat()
        var row = firstRow
        while (row <= lastRow) {
            val vertical = pose.eyeHeight + (row + 0.5 - metrics.horizonRow) * depth / metrics.focalRows - fixture.baseHeight
            var column = firstColumn
            while (column <= lastColumn) {
                val horizontal = (metrics.cameraPlaneXForColumn(column) * depth - leftLateral) / BILLBOARD_WIDTH
                val part = ParkFixtureSilhouette.partAt(
                    fixture.definition.tags,
                    horizontal,
                    vertical,
                    fixture.definition.height.toDouble(),
                )
                if (part != null) {
                    val glyph = part.glyph ?: materialSampler.glyph(
                        material,
                        fixture.position.x,
                        fixture.position.z,
                        vertical,
                        timeSeconds,
                        depth,
                    )
                    val foreground = part.paletteKey?.let { paletteKey ->
                        materialSampler.foreground(paletteKey, side = null, depth = depth)
                    } ?: materialSampler.foreground(
                        material,
                        side = null,
                        depth = depth,
                        worldX = fixture.position.x,
                        worldZ = fixture.position.z,
                        vertical = vertical,
                        timeSeconds = timeSeconds,
                    )
                    frame.writeIfNearer(
                        x = column,
                        y = row,
                        glyph = glyph,
                        foreground = foreground,
                        background = fixtureBackground?.let {
                            blendRgba(frame.backgroundAt(column, row), it, fixtureBackgroundWeight)
                        } ?: frame.backgroundAt(column, row),
                        depth = fixtureDepth,
                        emissive = part.emissiveStrength,
                    )
                }
                column += 1
            }
            row += 1
        }
    }

    private fun buildFixtures(grid: WorldGrid): Array<Fixture> {
        val fixtures = ArrayList<Fixture>()
        grid.forEachCell { cell, definition ->
            if (FIXTURE_TAG in definition.tags) {
                val position = WorldPosition(cell.x + 0.5, cell.z + 0.5)
                fixtures += Fixture(position, grid.terrainHeightAt(position.x, position.z), definition)
            }
        }
        return fixtures.toTypedArray()
    }

    private data class Fixture(
        val position: WorldPosition,
        val baseHeight: Double,
        val definition: CellDefinition,
    )

    private fun slotMachineBackgroundWeight(depth: Double): Double =
        ((SLOT_BACKGROUND_END_DISTANCE - depth) /
            (SLOT_BACKGROUND_END_DISTANCE - SLOT_BACKGROUND_FULL_DISTANCE)).coerceIn(0.0, 1.0)

    private fun blendRgba(from: Int, to: Int, amount: Double): Int {
        if (amount <= 0.0) return from
        if (amount >= 1.0) return to
        val inverse = 1.0 - amount
        val red = (((from ushr 24) and 0xFF) * inverse + ((to ushr 24) and 0xFF) * amount).toInt()
        val green = (((from ushr 16) and 0xFF) * inverse + ((to ushr 16) and 0xFF) * amount).toInt()
        val blue = (((from ushr 8) and 0xFF) * inverse + ((to ushr 8) and 0xFF) * amount).toInt()
        val alpha = ((from and 0xFF) * inverse + (to and 0xFF) * amount).toInt()
        return red shl 24 or (green shl 16) or (blue shl 8) or alpha
    }

    private companion object {
        const val FIXTURE_TAG = "fixture"
        const val SLOT_MACHINE_TAG = "slot-machine"
        const val SLOT_BACKGROUND_FULL_DISTANCE = 6.0
        const val SLOT_BACKGROUND_END_DISTANCE = 10.0
        const val BILLBOARD_WIDTH = 1.0
        const val MINIMUM_DEPTH = 1e-6
    }
}
