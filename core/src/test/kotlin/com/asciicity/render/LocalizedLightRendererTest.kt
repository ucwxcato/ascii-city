package com.asciicity.render

import java.lang.management.ManagementFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class LocalizedLightRendererTest {
    @Test
    fun `bright source tints only finite cells at or behind its resolved depth`() {
        val frame = AsciiFrame(5, 3)
        frame.clear(background = 0x05070CFF)
        frame.set(2, 1, 'o'.code, 0xFFB02EFF.toInt(), 0x05070CFF, depth = 8f, emissive = 1f)
        frame.set(3, 1, '.'.code, 0xA0A0A0FF.toInt(), 0x05070CFF, depth = 9f)
        frame.set(1, 1, '.'.code, 0xA0A0A0FF.toInt(), 0x05070CFF, depth = 7f)

        val writes = LocalizedLightRenderer().render(frame)

        assertTrue(writes > 0)
        assertNotEquals(0x05070CFF, frame.backgroundAt(3, 1), "Farther visible geometry receives local tint")
        assertEquals(0x05070CFF, frame.backgroundAt(1, 1), "Nearer geometry must not receive hidden-source tint")
        assertEquals(0x05070CFF, frame.backgroundAt(0, 0), "Infinite-depth sky remains untouched")
    }

    @Test
    fun `subthreshold emissive cells do not create a local light field`() {
        val frame = AsciiFrame(3, 1)
        frame.clear(background = 0x05070CFF)
        frame.set(0, 0, '*'.code, 0x22EE55FF, 0x05070CFF, depth = 4f, emissive = 0.84f)
        frame.set(1, 0, '.'.code, 0xA0A0A0FF.toInt(), 0x05070CFF, depth = 5f)

        assertEquals(0, LocalizedLightRenderer().render(frame))
        assertEquals(0x05070CFF, frame.backgroundAt(1, 0))
    }

    @Test
    fun `fine local lighting stays allocation bounded after warmup`() {
        val allocationBean = ManagementFactory.getThreadMXBean() as? com.sun.management.ThreadMXBean ?: return
        if (!allocationBean.isThreadAllocatedMemorySupported) return
        allocationBean.isThreadAllocatedMemoryEnabled = true
        val frame = AsciiFrame(213, 60)
        for (index in 0 until frame.cellCount) {
            frame.set(
                x = index % frame.width,
                y = index / frame.width,
                glyph = '.'.code,
                foreground = 0x8090A0FF.toInt(),
                background = 0x05070CFF,
                depth = 6f + index % 20,
                emissive = if (index % 97 == 0) 1f else 0f,
            )
        }
        val renderer = LocalizedLightRenderer()
        repeat(1_000) { renderer.render(frame) }

        val threadId = Thread.currentThread().threadId()
        val before = allocationBean.getThreadAllocatedBytes(threadId)
        repeat(3_600) { renderer.render(frame) }
        val allocated = allocationBean.getThreadAllocatedBytes(threadId) - before

        assertTrue(allocated <= 4_096, "steady local lighting allocated $allocated bytes")
    }
}
