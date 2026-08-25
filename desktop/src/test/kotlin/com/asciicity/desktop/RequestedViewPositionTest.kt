package com.asciicity.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class RequestedViewPositionTest {
    @Test
    fun `optional view position requires finite paired coordinates`() {
        assertNull(RequestedViewPosition.from(emptyArray()))
        assertEquals(RequestedViewPosition(51.5, 19.2), RequestedViewPosition.from(arrayOf("--view-x=51.5", "--view-z=19.2")))
        assertFailsWith<IllegalArgumentException> { RequestedViewPosition.from(arrayOf("--view-x=51.5")) }
        assertFailsWith<IllegalArgumentException> { RequestedViewPosition.from(arrayOf("--view-z=19.2")) }
        assertFailsWith<IllegalArgumentException> {
            RequestedViewPosition.from(arrayOf("--view-x=NaN", "--view-z=19.2"))
        }
    }
}
