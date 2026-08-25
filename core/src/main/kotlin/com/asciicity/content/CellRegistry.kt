package com.asciicity.content

import com.asciicity.world.CellDefinition

class CellRegistry private constructor(
    private val definitions: List<CellDefinition>,
) {
    private val indexById = definitions.indices.associateBy { definitions[it].id }
    private val indexByToken = definitions.indices.associateBy { definitions[it].token }

    val size: Int
        get() = definitions.size

    fun indexForId(id: String): Int = indexById[id]
        ?: throw IllegalArgumentException("Unknown cell definition '$id'")

    fun indexForToken(token: Char): Int = indexByToken[token]
        ?: throw IllegalArgumentException("Unknown cell token '$token'")

    fun definitionAt(index: Int): CellDefinition {
        require(index in definitions.indices) { "Cell definition index $index is outside 0 until ${definitions.size}" }
        return definitions[index]
    }

    fun definitionForId(id: String): CellDefinition = definitionAt(indexForId(id))

    fun definitionForToken(token: Char): CellDefinition = definitionAt(indexForToken(token))

    fun containsToken(token: Char): Boolean = token in indexByToken

    fun all(): List<CellDefinition> = definitions.toList()

    companion object {
        fun create(
            definitions: List<CellDefinition>,
            materials: MaterialRegistry,
            source: String = "cells",
        ): CellRegistry {
            if (definitions.isEmpty()) {
                throw ContentValidationException("$source:$.cells: at least one cell definition is required")
            }
            val ids = mutableSetOf<String>()
            val tokens = mutableSetOf<Char>()
            for ((index, definition) in definitions.withIndex()) {
                val path = "$.cells[$index]"
                ContentIds.requireStable(definition.id, source, "$path.id")
                if (!ids.add(definition.id)) {
                    throw ContentValidationException("$source:$path.id: duplicate cell ID '${definition.id}'")
                }
                if (definition.token.code !in 32..126) {
                    throw ContentValidationException("$source:$path.token: must be one printable ASCII character")
                }
                if (!tokens.add(definition.token)) {
                    throw ContentValidationException("$source:$path.token: duplicate cell token '${definition.token}'")
                }
                if (!materials.contains(definition.groundMaterialId)) {
                    throw ContentValidationException(
                        "$source:$path.groundMaterial: unknown material '${definition.groundMaterialId}'",
                    )
                }
                if (!materials.contains(definition.facadeMaterialId)) {
                    throw ContentValidationException(
                        "$source:$path.facadeMaterial: unknown material '${definition.facadeMaterialId}'",
                    )
                }
                if (definition.height < 0) {
                    throw ContentValidationException("$source:$path.height: must be non-negative")
                }
                if ((definition.solid || definition.opaque) && definition.height == 0) {
                    throw ContentValidationException("$source:$path.height: solid or opaque cells require positive height")
                }
            }
            return CellRegistry(definitions)
        }
    }
}
