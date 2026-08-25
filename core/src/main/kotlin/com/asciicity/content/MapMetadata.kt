package com.asciicity.content

import com.asciicity.entity.PlayerState

data class MapMetadata(
    val id: String,
    val version: Int,
    val playerSpawn: PlayerState,
    val skyPaletteKey: String,
    val fogPaletteKey: String,
    val objectsFile: String,
    val ceiling: MapCeiling? = null,
)
