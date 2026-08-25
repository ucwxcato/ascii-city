package com.asciicity.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AsciiFrameTest {
    @Test
    fun `clear and corner writes preserve all primitive cell fields`() {
        val frame = AsciiFrame(3, 2)
        frame.clear('.'.code, 0x11223344, 0x55667788, 9f)
        frame.set(2, 1, '@'.code, 0xAABBCCDD.toInt(), 0x01020304, 2f)

        assertEquals('.'.code, frame.glyphAt(0, 0))
        assertEquals(0x11223344, frame.foregroundAt(0, 0))
        assertEquals(0x55667788, frame.backgroundAt(0, 0))
        assertEquals(9f, frame.depthAt(0, 0))
        assertEquals('@'.code, frame.glyphAt(2, 1))
        assertEquals(2f, frame.depthAt(2, 1))
        assertEquals(0f, frame.emissiveAt(2, 1))
        assertEquals(6, frame.cellCount)
    }

    @Test
    fun `emissive strength follows the winning depth cell`() {
        val frame = AsciiFrame(1, 1)

        assertTrue(frame.writeIfNearer(0, 0, '*'.code, 0x11EE44FF, 0x000000FF, 4f, emissive = 0.9f))
        assertEquals(0.9f, frame.emissiveAt(0, 0))
        assertFalse(frame.writeIfNearer(0, 0, 'o'.code, 0xFFFFFFFF.toInt(), 0x000000FF, 5f))
        assertEquals(0.9f, frame.emissiveAt(0, 0))
        assertTrue(frame.writeIfNearer(0, 0, '#'.code, 0x777777FF, 0x000000FF, 2f))
        assertEquals(0f, frame.emissiveAt(0, 0))
    }

    @Test
    fun `nearer depth wins and equal or farther depth does not overwrite`() {
        val frame = AsciiFrame(1, 1)
        assertTrue(frame.writeIfNearer(0, 0, '#'.code, 1, 2, 5f))
        assertFalse(frame.writeIfNearer(0, 0, 'X'.code, 3, 4, 5f))
        assertFalse(frame.writeIfNearer(0, 0, 'X'.code, 3, 4, 6f))
        assertTrue(frame.writeIfNearer(0, 0, '@'.code, 5, 6, 4f))
        assertEquals('@'.code, frame.glyphAt(0, 0))
        assertEquals(4f, frame.depthAt(0, 0))
    }

    @Test
    fun `invalid dimensions coordinates and nan depth fail clearly`() {
        assertFailsWith<IllegalArgumentException> { AsciiFrame(0, 1) }
        val frame = AsciiFrame(2, 2)
        assertFailsWith<IllegalArgumentException> { frame.glyphAt(-1, 0) }
        assertFailsWith<IllegalArgumentException> { frame.glyphAt(2, 1) }
        assertFailsWith<IllegalArgumentException> {
            frame.set(0, 0, '@'.code, 0, 0, Float.NaN)
        }
        assertFailsWith<IllegalArgumentException> {
            frame.set(0, 0, '@'.code, 0, 0, 1f, emissive = 1.1f)
        }
    }
}
