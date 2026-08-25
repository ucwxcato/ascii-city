package com.asciicity.world

data class CellDefinition(
    val id: String,
    val token: Char,
    val groundMaterialId: String,
    val facadeMaterialId: String,
    val solid: Boolean,
    val opaque: Boolean,
    val height: Int,
    val tags: Set<String>,
    val interactionType: String?,
)
