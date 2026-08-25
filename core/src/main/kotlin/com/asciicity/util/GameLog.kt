package com.asciicity.util

import com.badlogic.gdx.Gdx

object GameLog {
    fun info(tag: String, message: String) {
        val application = Gdx.app
        if (application != null) {
            application.log(tag, message)
        } else {
            System.out.println("[$tag] $message")
        }
    }

    fun error(tag: String, message: String, throwable: Throwable) {
        val application = Gdx.app
        if (application != null) {
            application.error(tag, message, throwable)
        } else {
            System.err.println("[$tag] $message")
            throwable.printStackTrace(System.err)
        }
    }
}
