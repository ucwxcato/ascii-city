package com.asciicity.render

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AsciiTestCardTest {
    private val palette = RenderPalette.parse(Files.readString(Path.of("data", "palette.json")))

    @Test
    fun `test card fills both supported surfaces deterministically`() {
        for ((width, height) in listOf(96 to 30, 128 to 40)) {
            val first = AsciiFrame(width, height)
            val second = AsciiFrame(width, height)
            AsciiTestCard.fill(first, palette)
            AsciiTestCard.fill(second, palette)

            for (y in 0 until height) {
                for (x in 0 until width) {
                    assertEquals(first.glyphAt(x, y), second.glyphAt(x, y))
                    assertEquals(first.foregroundAt(x, y), second.foregroundAt(x, y))
                    assertEquals(first.backgroundAt(x, y), second.backgroundAt(x, y))
                }
            }
            assertEquals('+'.code, first.glyphAt(0, 0))
            assertEquals('+'.code, first.glyphAt(width - 1, height - 1))
            assertTrue((0 until width).any { first.glyphAt(it, height / 3) != ' '.code })
        }
    }
}
