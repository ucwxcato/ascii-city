package com.asciicity.desktop

import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopWindowSpecTest {
    @Test
    fun `default window remains the sixteen by nine reference size`() {
        assertEquals(1280, DesktopWindowSpec.DEFAULT_WIDTH)
        assertEquals(720, DesktopWindowSpec.DEFAULT_HEIGHT)
        assertEquals(16 * DesktopWindowSpec.DEFAULT_HEIGHT, 9 * DesktopWindowSpec.DEFAULT_WIDTH)
    }

    @Test
    fun `window keeps its minimum but declares no upper size limit`() {
        assertEquals(1280, DesktopWindowSpec.MINIMUM_WIDTH)
        assertEquals(720, DesktopWindowSpec.MINIMUM_HEIGHT)
        assertEquals(16 * DesktopWindowSpec.MINIMUM_HEIGHT, 9 * DesktopWindowSpec.MINIMUM_WIDTH)
        assertEquals(-1, DesktopWindowSpec.UNBOUNDED_SIZE)
    }
}
