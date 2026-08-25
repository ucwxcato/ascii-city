package com.asciicity.render

object FontMetrics {
    const val CELL_WIDTH = 10
    const val CELL_HEIGHT = 18
    const val BASELINE_FROM_CELL_BOTTOM = 18
    const val MINIMUM_COLUMNS = 96
    const val MINIMUM_ROWS = 30
    const val REFERENCE_COLUMNS = 128
    const val REFERENCE_ROWS = 40

    fun validateDefinition(definition: String) = AsciiFontProfile.STANDARD.validateDefinition(definition)
}
