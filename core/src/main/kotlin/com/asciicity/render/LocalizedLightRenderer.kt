package com.asciicity.render

/**
 * Applies a compact, depth-aware background tint around bright resolved world glyphs.
 * It owns no world state: source eligibility comes solely from the final frame emissive channel.
 */
internal class LocalizedLightRenderer {
    fun render(frame: AsciiFrame): Int {
        var tintedCells = 0
        for (sourceIndex in 0 until frame.cellCount) {
            val sourceStrength = frame.emissiveAtIndex(sourceIndex)
            val sourceDepth = frame.depthAtIndex(sourceIndex)
            if (sourceStrength < MINIMUM_SOURCE_EMISSIVE || !sourceDepth.isFinite()) continue

            val sourceColumn = sourceIndex % frame.width
            val sourceRow = sourceIndex / frame.width
            val radius = if (sourceStrength >= WIDE_SOURCE_EMISSIVE) WIDE_RADIUS else COMPACT_RADIUS
            val radiusSquared = radius * radius
            for (row in (sourceRow - radius)..(sourceRow + radius)) {
                if (row !in 0 until frame.height) continue
                for (column in (sourceColumn - radius)..(sourceColumn + radius)) {
                    if (column !in 0 until frame.width) continue
                    val targetIndex = row * frame.width + column
                    if (targetIndex == sourceIndex) continue
                    val targetDepth = frame.depthAtIndex(targetIndex)
                    if (!targetDepth.isFinite() || targetDepth + DEPTH_EPSILON < sourceDepth) continue
                    val offsetX = column - sourceColumn
                    val offsetY = row - sourceRow
                    val distanceSquared = offsetX * offsetX + offsetY * offsetY
                    if (distanceSquared > radiusSquared) continue

                    val falloff = 1.0 - distanceSquared.toDouble() / (radiusSquared + 1.0)
                    val amount = sourceStrength * falloff.toFloat() * MAXIMUM_TINT_STRENGTH
                    val background = frame.backgroundAtIndex(targetIndex)
                    val tinted = blend(background, frame.foregroundAtIndex(sourceIndex), amount)
                    if (tinted != background) {
                        frame.setBackgroundAtIndex(targetIndex, tinted)
                        tintedCells += 1
                    }
                }
            }
        }
        return tintedCells
    }

    private fun blend(from: Int, to: Int, amount: Float): Int {
        val inverse = 1f - amount
        val red = (((from ushr 24) and 0xFF) * inverse + ((to ushr 24) and 0xFF) * amount).toInt()
        val green = (((from ushr 16) and 0xFF) * inverse + ((to ushr 16) and 0xFF) * amount).toInt()
        val blue = (((from ushr 8) and 0xFF) * inverse + ((to ushr 8) and 0xFF) * amount).toInt()
        return red shl 24 or (green shl 16) or (blue shl 8) or (from and 0xFF)
    }

    private companion object {
        const val MINIMUM_SOURCE_EMISSIVE = 0.85f
        const val WIDE_SOURCE_EMISSIVE = 0.98f
        const val COMPACT_RADIUS = 1
        const val WIDE_RADIUS = 2
        const val MAXIMUM_TINT_STRENGTH = 0.18f
        const val DEPTH_EPSILON = 0.02f
    }
}
