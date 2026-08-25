package com.asciicity.render

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.JsonReader

class PaletteValidationException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)

class RenderPalette private constructor(
    private val colors: Map<String, Int>,
) {
    operator fun get(key: String): Int = colors[key]
        ?: throw PaletteValidationException("Missing palette key '$key'")

    fun colorOrFallback(key: String): Int = colors[key] ?: FALLBACK_RGBA

    companion object {
        const val FALLBACK_RGBA: Int = 0xFF00FFFF.toInt()

        fun load(file: FileHandle): RenderPalette {
            val text = try {
                file.readString("UTF-8")
            } catch (error: Exception) {
                throw PaletteValidationException("Could not read palette '${file.path()}': ${error.message}", error)
            }
            return parse(text, file.path())
        }

        fun parse(text: String, sourceName: String = "palette"): RenderPalette {
            val root = try {
                JsonReader().parse(text)
            } catch (error: Exception) {
                throw PaletteValidationException("Malformed palette JSON in '$sourceName': ${error.message}", error)
            }
            if (!root.isObject) {
                throw PaletteValidationException("Palette '$sourceName' must contain a JSON object")
            }

            val parsed = linkedMapOf<String, Int>()
            var entry = root.child
            while (entry != null) {
                val key = entry.name
                    ?: throw PaletteValidationException("Palette '$sourceName' contains an unnamed entry")
                if (key in parsed) {
                    throw PaletteValidationException("Duplicate palette key '$key' in '$sourceName'")
                }
                if (!entry.isString) {
                    throw PaletteValidationException("Palette key '$key' in '$sourceName' must be a hex color string")
                }
                parsed[key] = parseRgba(entry.asString(), key, sourceName)
                entry = entry.next
            }

            for (requiredKey in PaletteKeys.REQUIRED) {
                if (requiredKey !in parsed) {
                    throw PaletteValidationException("Missing palette key '$requiredKey' in '$sourceName'")
                }
            }
            return RenderPalette(parsed.toMap())
        }

        private fun parseRgba(value: String, key: String, sourceName: String): Int {
            val digits = value.removePrefix("#")
            if (digits.length != 6 && digits.length != 8 || digits.any { !it.isDigit() && it.lowercaseChar() !in 'a'..'f' }) {
                throw PaletteValidationException(
                    "Palette key '$key' in '$sourceName' must use #RRGGBB or #RRGGBBAA, but was '$value'",
                )
            }
            val rgbaDigits = if (digits.length == 6) "${digits}FF" else digits
            return rgbaDigits.toLong(16).toInt()
        }
    }
}
