package com.asciicity.render

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RenderPaletteTest {
    @Test
    fun `bundled palette contains every semantic color`() {
        val text = Files.readString(Path.of("data", "palette.json"))
        val palette = RenderPalette.parse(text, "palette.json")

        for (key in PaletteKeys.REQUIRED) {
            palette[key]
        }
        assertEquals(RenderPalette.FALLBACK_RGBA, palette.colorOrFallback("future-material"))
    }

    @Test
    fun `missing duplicate malformed and invalid keys identify the problem`() {
        val missing = assertFailsWith<PaletteValidationException> { RenderPalette.parse("{}") }
        assertTrue(missing.message.orEmpty().contains("sky"))

        val duplicate = assertFailsWith<PaletteValidationException> {
            RenderPalette.parse("{\"sky\":\"#000000\",\"sky\":\"#FFFFFF\"}")
        }
        assertTrue(duplicate.message.orEmpty().contains("Duplicate palette key 'sky'"))

        val malformed = assertFailsWith<PaletteValidationException> { RenderPalette.parse("{") }
        assertTrue(malformed.message.orEmpty().contains("Malformed palette JSON"))

        val invalid = assertFailsWith<PaletteValidationException> {
            RenderPalette.parse("{\"sky\":\"not-a-color\"}")
        }
        assertTrue(invalid.message.orEmpty().contains("palette key 'sky'", ignoreCase = true))
    }
}
