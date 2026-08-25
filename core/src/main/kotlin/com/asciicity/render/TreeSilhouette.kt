package com.asciicity.render

internal object TreeSilhouette {
    enum class Part {
        TRUNK,
        FOLIAGE,
    }

    fun partAt(
        faceCoordinate: Double,
        vertical: Double,
        height: Double,
        pine: Boolean = false,
    ): Part? {
        if (vertical < 0.0 || vertical > height) return null
        if (pine) return pinePartAt(faceCoordinate, vertical, height)
        val horizontalDistance = kotlin.math.abs(faceCoordinate - 0.5)
        val canopyBottom = height * CANOPY_BOTTOM_RATIO
        if (vertical >= canopyBottom && horizontalDistance <= canopyHalfWidth(vertical, height)) {
            return Part.FOLIAGE
        }
        if (vertical <= height * TRUNK_HEIGHT_RATIO && horizontalDistance <= TRUNK_HALF_WIDTH) {
            return Part.TRUNK
        }
        return null
    }

    private fun pinePartAt(faceCoordinate: Double, vertical: Double, height: Double): Part? {
        val horizontalDistance = kotlin.math.abs(faceCoordinate - 0.5)
        val normalized = vertical / height
        if (normalized >= PINE_CANOPY_BOTTOM_RATIO) {
            val taper = (normalized - PINE_CANOPY_BOTTOM_RATIO) / (1.0 - PINE_CANOPY_BOTTOM_RATIO)
            val halfWidth = PINE_CANOPY_BASE_HALF_WIDTH +
                taper * (PINE_CANOPY_TOP_HALF_WIDTH - PINE_CANOPY_BASE_HALF_WIDTH)
            if (horizontalDistance <= halfWidth) return Part.FOLIAGE
        }
        if (normalized <= PINE_TRUNK_HEIGHT_RATIO && horizontalDistance <= PINE_TRUNK_HALF_WIDTH) {
            return Part.TRUNK
        }
        return null
    }

    private fun canopyHalfWidth(vertical: Double, height: Double): Double {
        val normalized = vertical / height
        return when {
            normalized < CANOPY_FULL_RATIO -> {
                val growth = (normalized - CANOPY_BOTTOM_RATIO) / (CANOPY_FULL_RATIO - CANOPY_BOTTOM_RATIO)
                CANOPY_BASE_HALF_WIDTH + growth * (CANOPY_MAX_HALF_WIDTH - CANOPY_BASE_HALF_WIDTH)
            }
            normalized < CANOPY_TAPER_RATIO -> CANOPY_MAX_HALF_WIDTH
            else -> {
                val taper = (normalized - CANOPY_TAPER_RATIO) / (1.0 - CANOPY_TAPER_RATIO)
                CANOPY_MAX_HALF_WIDTH + taper * (CANOPY_TOP_HALF_WIDTH - CANOPY_MAX_HALF_WIDTH)
            }
        }
    }

    private const val TRUNK_HEIGHT_RATIO = 0.43
    private const val TRUNK_HALF_WIDTH = 0.10
    private const val CANOPY_BOTTOM_RATIO = 0.27
    private const val CANOPY_FULL_RATIO = 0.44
    private const val CANOPY_TAPER_RATIO = 0.72
    private const val CANOPY_BASE_HALF_WIDTH = 0.23
    private const val CANOPY_MAX_HALF_WIDTH = 0.49
    private const val CANOPY_TOP_HALF_WIDTH = 0.08
    private const val PINE_TRUNK_HEIGHT_RATIO = 0.28
    private const val PINE_TRUNK_HALF_WIDTH = 0.07
    private const val PINE_CANOPY_BOTTOM_RATIO = 0.12
    private const val PINE_CANOPY_BASE_HALF_WIDTH = 0.47
    private const val PINE_CANOPY_TOP_HALF_WIDTH = 0.035
}
