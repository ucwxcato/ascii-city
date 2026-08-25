package com.asciicity.desktop

import java.io.PrintStream

class FatalErrorHandler(
    private val errorOutput: PrintStream = System.err,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        errorOutput.println("[ASCII City] Fatal error on thread '${thread.name}': ${throwable.message}")
        throwable.printStackTrace(errorOutput)
    }
}
