package com.asciicity.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class RequestedWindowSizeTest {
    @Test
    fun `returns null when no size was requested`() {
        assertNull(RequestedWindowSize.from(arrayOf("--smoke-test")))
    }

    @Test
    fun `parses a requested size`() {
        assertEquals(
            RequestedWindowSize(960, 540),
            RequestedWindowSize.from(arrayOf("--window-size=960x540")),
        )
    }

    @Test
    fun `accepts an ultrawide size above the old cap`() {
        assertEquals(
            RequestedWindowSize(3440, 1440),
            RequestedWindowSize.from(arrayOf("--window-size=3440x1440")),
        )
    }

    @Test
    fun `rejects malformed or non-positive sizes`() {
        assertFailsWith<IllegalArgumentException> {
            RequestedWindowSize.from(arrayOf("--window-size=wide"))
        }
        assertFailsWith<IllegalArgumentException> {
            RequestedWindowSize.from(arrayOf("--window-size=0x540"))
        }
    }
}
