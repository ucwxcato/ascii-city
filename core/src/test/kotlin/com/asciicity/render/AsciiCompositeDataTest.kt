package com.asciicity.render

import java.lang.management.ManagementFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AsciiCompositeDataTest {
    @Test
    fun `depth target encodes normalized finite depth with explicit validity`() {
        val frame = AsciiFrame(3, 1)
        frame.set(0, 0, '#'.code, 0xFFFFFFFF.toInt(), 0, 12f)
        frame.set(1, 0, '#'.code, 0xFFFFFFFF.toInt(), 0, 96f)
        val data = AsciiCompositeData(maximumDepth = 48f)

        data.update(frame)

        assertEquals(3, data.width)
        assertEquals(1, data.height)
        assertEquals(2, data.finiteDepthCellCount)
        assertEquals(0x404040FF, data.depthPixelAt(0))
        assertEquals(0xFFFFFFFF.toInt(), data.depthPixelAt(1))
        assertEquals(0x00000000, data.depthPixelAt(2))
    }

    @Test
    fun `emissive target carries explicit glyph color and strength only`() {
        val frame = AsciiFrame(3, 1)
        frame.set(0, 0, '*'.code, 0x12E45AFF, 0, 4f, emissive = 0.5f)
        frame.set(1, 0, ' '.code, 0xEE22AAFF.toInt(), 0, 4f, emissive = 1f)
        frame.set(2, 0, '#'.code, 0xFFFFFFFF.toInt(), 0, 4f)
        val data = AsciiCompositeData()

        data.update(frame)

        assertEquals(1, data.emissiveCellCount)
        assertEquals(0x12E45A80, data.emissivePixelAt(0))
        assertEquals(0, data.emissivePixelAt(1))
        assertEquals(0, data.emissivePixelAt(2))
    }

    @Test
    fun `buffers resize only with the selected ascii surface`() {
        val data = AsciiCompositeData()
        data.update(AsciiFrame(128, 40))
        assertEquals(128, data.width)
        assertEquals(40, data.height)

        data.update(AsciiFrame(160, 48))
        assertEquals(160, data.width)
        assertEquals(48, data.height)
    }

    @Test
    fun `emissive input is fully replaced when a later frame clears it`() {
        val frame = AsciiFrame(1, 1)
        val data = AsciiCompositeData()
        frame.set(0, 0, '*'.code, 0x22EE55FF, 0, 2f, emissive = 1f)
        data.update(frame)
        assertEquals(1, data.emissiveCellCount)

        frame.clear()
        data.update(frame)

        assertEquals(0, data.emissiveCellCount)
        assertEquals(0, data.emissivePixelAt(0))
    }

    @Test
    fun `fine composite inputs allocate no per cell objects in steady state`() {
        val allocationBean = ManagementFactory.getThreadMXBean() as? com.sun.management.ThreadMXBean ?: return
        if (!allocationBean.isThreadAllocatedMemorySupported) return
        allocationBean.isThreadAllocatedMemoryEnabled = true
        val frame = AsciiFrame(213, 60)
        for (row in 0 until frame.height) {
            for (column in 0 until frame.width) {
                frame.set(
                    x = column,
                    y = row,
                    glyph = if ((column + row) % 11 == 0) '*'.code else '#'.code,
                    foreground = 0x35E6FFFF,
                    background = 0x05070CFF,
                    depth = 1f + (column % 47),
                    emissive = if ((column + row) % 11 == 0) 0.8f else 0f,
                )
            }
        }
        val data = AsciiCompositeData()
        repeat(1_000) { data.update(frame) }

        val threadId = Thread.currentThread().threadId()
        val before = allocationBean.getThreadAllocatedBytes(threadId)
        repeat(3_600) { data.update(frame) }
        val allocatedBytes = allocationBean.getThreadAllocatedBytes(threadId) - before

        assertTrue(allocatedBytes <= 4_096, "steady Fine composite input update allocated $allocatedBytes bytes")
    }

    @Test
    fun `maximum depth must be positive and finite`() {
        assertFailsWith<IllegalArgumentException> { AsciiCompositeData(0f) }
        assertFailsWith<IllegalArgumentException> { AsciiCompositeData(Float.POSITIVE_INFINITY) }
    }
}
