package com.asciicity.render

import kotlin.math.roundToInt

/**
 * Reusable cell-resolution inputs for the one world compositor. Depth alpha is the explicit
 * validity bit; emissive alpha is the authored presentation strength. No UI data enters here.
 */
class AsciiCompositeData(
    private val maximumDepth: Float = DEFAULT_MAXIMUM_DEPTH,
) {
    private var depthPixels = IntArray(0)
    private var emissivePixels = IntArray(0)

    var width: Int = 0
        private set
    var height: Int = 0
        private set
    var finiteDepthCellCount: Int = 0
        private set
    var emissiveCellCount: Int = 0
        private set

    init {
        require(maximumDepth.isFinite() && maximumDepth > 0f) { "Composite maximum depth must be positive and finite" }
    }

    fun update(frame: AsciiFrame) {
        ensureCapacity(frame.width, frame.height)
        var finiteDepthCells = 0
        var emissiveCells = 0
        for (index in 0 until frame.cellCount) {
            val depth = frame.depthAtIndex(index)
            depthPixels[index] = if (depth.isFinite() && depth > 0f) {
                finiteDepthCells += 1
                val channel = ((depth / maximumDepth).coerceIn(0f, 1f) * 255f).roundToInt()
                channel shl 24 or (channel shl 16) or (channel shl 8) or 0xFF
            } else {
                INVALID_DEPTH_PIXEL
            }

            val emissive = frame.emissiveAtIndex(index)
            emissivePixels[index] = if (emissive > 0f && frame.glyphAtIndex(index) != ' '.code) {
                emissiveCells += 1
                val alpha = (emissive * 255f).roundToInt().coerceIn(1, 255)
                frame.foregroundAtIndex(index) and 0xFFFFFF00.toInt() or alpha
            } else {
                TRANSPARENT_PIXEL
            }
        }
        finiteDepthCellCount = finiteDepthCells
        emissiveCellCount = emissiveCells
    }

    internal fun depthPixelAt(index: Int): Int = depthPixels[index]

    internal fun emissivePixelAt(index: Int): Int = emissivePixels[index]

    private fun ensureCapacity(nextWidth: Int, nextHeight: Int) {
        if (width == nextWidth && height == nextHeight) return
        width = nextWidth
        height = nextHeight
        val size = Math.multiplyExact(width, height)
        depthPixels = IntArray(size)
        emissivePixels = IntArray(size)
    }

    private companion object {
        const val DEFAULT_MAXIMUM_DEPTH = 48f
        const val INVALID_DEPTH_PIXEL = 0x00000000
        const val TRANSPARENT_PIXEL = 0x00000000
    }
}
