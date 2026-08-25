package com.asciicity.desktop

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertContains

class FatalErrorHandlerTest {
    @Test
    fun `fatal handler reports thread message and stack trace`() {
        val outputBytes = ByteArrayOutputStream()
        val handler = FatalErrorHandler(PrintStream(outputBytes, true, Charsets.UTF_8))

        handler.uncaughtException(Thread("test-thread"), IllegalStateException("test failure"))

        val output = outputBytes.toString(Charsets.UTF_8)
        assertContains(output, "test-thread")
        assertContains(output, "test failure")
        assertContains(output, "IllegalStateException")
    }
}
