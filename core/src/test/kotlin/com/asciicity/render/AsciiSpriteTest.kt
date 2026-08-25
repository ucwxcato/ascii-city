package com.asciicity.render

import com.asciicity.TestContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AsciiSpriteTest {
    @Test
    fun `rin sprite exposes immutable top-first glyph and color cells`() {
        val content = TestContent.loaded
        val sprite = content.sprites.sprite("sprite.rin")

        assertEquals(9, sprite.columns)
        assertEquals(10, sprite.rows)
        assertEquals(0.5, sprite.anchorX)
        assertEquals(0.0, sprite.anchorY)
        assertEquals(0.8, sprite.physicalWidth)
        assertEquals(1.8, sprite.physicalHeight)
        assertTrue(sprite.isTransparent(0, 0))
        assertFalse(sprite.isTransparent(3, 0))
        assertEquals('/'.code, sprite.glyphAt(3, 0))
        assertEquals(content.palette[PaletteKeys.NEON_CYAN], sprite.foregroundAt(3, 0))
        assertFailsWith<IllegalArgumentException> { sprite.glyphAt(sprite.columns, 0) }
        assertFailsWith<IllegalArgumentException> { sprite.foregroundAt(0, sprite.rows) }
    }

    @Test
    fun `sprite registry rejects unknown ids`() {
        assertFailsWith<IllegalArgumentException> {
            TestContent.loaded.sprites.sprite("sprite.missing")
        }
    }
}
