package com.asciicity.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.ScreenViewport

class ErrorOverlay(message: String) : Disposable {
    private val fallbackFont = BitmapFont()
    private val stage = Stage(ScreenViewport())

    init {
        fallbackFont.data.setScale(1.35f)
        val style = Label.LabelStyle(fallbackFont, Color(1f, 0.28f, 0.44f, 1f))
        val title = Label("ASCII CITY", style)
        val status = Label("ASSET LOAD FAILED", style)
        val details = Label(message, style)
        details.setWrap(true)
        details.setAlignment(Align.center)
        val hint = Label("Check the assets folder and application log.", style)

        val table = Table()
        table.setFillParent(true)
        table.pad(48f)
        table.add(title).center().row()
        table.add(status).padTop(24f).center().row()
        table.add(details).padTop(24f).growX().center().row()
        table.add(hint).padTop(24f).center()
        stage.addActor(table)
    }

    fun render(delta: Float) {
        stage.act(delta.coerceAtMost(1f / 15f))
        stage.draw()
    }

    fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun dispose() {
        stage.dispose()
        fallbackFont.dispose()
    }
}
