package com.asciicity.audio

import com.asciicity.world.WorldGrid
import com.asciicity.world.WorldPosition
import kotlin.math.hypot

internal class WaterfallAudioField private constructor(
    private val sourceX: DoubleArray,
    private val sourceZ: DoubleArray,
    private val fullVolumeDistance: Double,
    private val silentDistance: Double,
) {
    val sourceCount: Int
        get() = sourceX.size

    fun mixAt(position: WorldPosition): Float {
        var nearestDistance = Double.POSITIVE_INFINITY
        for (index in sourceX.indices) {
            nearestDistance = minOf(
                nearestDistance,
                hypot(position.x - sourceX[index], position.z - sourceZ[index]),
            )
        }
        if (nearestDistance <= fullVolumeDistance) return 1f
        if (nearestDistance >= silentDistance) return 0f
        val fade = (nearestDistance - fullVolumeDistance) / (silentDistance - fullVolumeDistance)
        val smoothFade = fade * fade * (3.0 - 2.0 * fade)
        return (1.0 - smoothFade).toFloat()
    }

    companion object {
        private const val WATERFALL_TAG = "waterfall"
        private const val DEFAULT_FULL_VOLUME_DISTANCE = 2.5
        private const val DEFAULT_SILENT_DISTANCE = 14.0

        fun fromGrid(grid: WorldGrid): WaterfallAudioField {
            val sources = mutableListOf<WorldPosition>()
            grid.forEachCell { cell, definition ->
                if (WATERFALL_TAG in definition.tags) {
                    sources += WorldPosition(cell.x + 0.5, cell.z + 0.5)
                }
            }
            return fromPositions(sources)
        }

        internal fun fromPositions(
            sources: List<WorldPosition>,
            fullVolumeDistance: Double = DEFAULT_FULL_VOLUME_DISTANCE,
            silentDistance: Double = DEFAULT_SILENT_DISTANCE,
        ): WaterfallAudioField {
            require(fullVolumeDistance >= 0.0 && fullVolumeDistance.isFinite()) {
                "Waterfall full-volume distance must be finite and non-negative"
            }
            require(silentDistance > fullVolumeDistance && silentDistance.isFinite()) {
                "Waterfall silent distance must be finite and greater than the full-volume distance"
            }
            return WaterfallAudioField(
                sourceX = DoubleArray(sources.size) { sources[it].x },
                sourceZ = DoubleArray(sources.size) { sources[it].z },
                fullVolumeDistance = fullVolumeDistance,
                silentDistance = silentDistance,
            )
        }
    }
}
