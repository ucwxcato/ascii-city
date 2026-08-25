package com.asciicity.content

import com.asciicity.render.AsciiSprite

class AsciiSpriteRegistry private constructor(
    sprites: List<AsciiSprite>,
) {
    private val byId = sprites.associateBy(AsciiSprite::id)

    val size: Int
        get() = byId.size

    fun contains(id: String): Boolean = id in byId

    fun sprite(id: String): AsciiSprite = byId[id]
        ?: throw IllegalArgumentException("Unknown ASCII sprite '$id'")

    fun all(): List<AsciiSprite> = byId.values.toList()

    companion object {
        internal fun create(sprites: List<AsciiSprite>, source: String): AsciiSpriteRegistry {
            if (sprites.isEmpty()) {
                throw ContentValidationException("$source:$.sprites: at least one ASCII sprite is required")
            }
            val ids = mutableSetOf<String>()
            for ((index, sprite) in sprites.withIndex()) {
                val path = "$.sprites[$index]"
                ContentIds.requireStable(sprite.id, source, "$path.id")
                if (!ids.add(sprite.id)) {
                    throw ContentValidationException("$source:$path.id: duplicate ASCII sprite ID '${sprite.id}'")
                }
            }
            return AsciiSpriteRegistry(sprites)
        }
    }
}
