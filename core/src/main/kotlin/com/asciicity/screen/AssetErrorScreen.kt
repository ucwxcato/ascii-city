package com.asciicity.screen

import com.asciicity.ui.ErrorOverlay
import com.badlogic.gdx.ScreenAdapter

class AssetErrorScreen(message: String) : ScreenAdapter() {
    private val overlay = ErrorOverlay(message)

    override fun render(delta: Float) {
        overlay.render(delta)
    }

    override fun resize(width: Int, height: Int) {
        overlay.resize(width, height)
    }

    override fun dispose() {
        overlay.dispose()
    }
}
