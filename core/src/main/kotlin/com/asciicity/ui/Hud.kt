package com.asciicity.ui

import com.asciicity.render.AsciiViewport
import com.asciicity.render.PaletteKeys
import com.asciicity.render.RenderPalette
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.ScreenViewport

class Hud(
    font: BitmapFont,
    palette: RenderPalette,
    private val onToggleMusic: () -> Unit,
) : Disposable {
    private val stage = Stage(ScreenViewport())
    private val panelTexture = createPanelTexture()
    private val panelStyle = Label.LabelStyle(font, color(palette[PaletteKeys.HUD])).also {
        it.background = TextureRegionDrawable(TextureRegion(panelTexture))
    }
    private val plainStyle = Label.LabelStyle(font, color(palette[PaletteKeys.HUD]))
    private val audioButtonStyle = TextButton.TextButtonStyle().also {
        it.up = TextureRegionDrawable(TextureRegion(panelTexture))
        it.down = TextureRegionDrawable(TextureRegion(panelTexture))
        it.over = TextureRegionDrawable(TextureRegion(panelTexture))
        it.font = font
        it.fontColor = color(palette[PaletteKeys.HUD])
        it.disabledFontColor = color(palette[PaletteKeys.FOG])
    }
    private val location = Label("", panelStyle)
    private val objective = Label("", panelStyle)
    private val signal = Label("", panelStyle)
    private val controls = Label(HudText.CONTROLS, panelStyle)
    private val reticle = Label("+", plainStyle)
    private val prompt = Label("", panelStyle)
    private val debug = Label("", panelStyle)
    private val debugMap = Label("", panelStyle)
    private val audioButton = TextButton("", audioButtonStyle)
    private var screenWidth = 0
    private var screenHeight = 0
    private var glyphScale = 1
    private var asciiViewport: AsciiViewport? = null

    val inputProcessor: InputProcessor
        get() = stage

    init {
        stage.addActor(location)
        stage.addActor(objective)
        stage.addActor(signal)
        stage.addActor(controls)
        stage.addActor(reticle)
        stage.addActor(prompt)
        stage.addActor(debug)
        stage.addActor(debugMap)
        audioButton.pad(2f, 6f, 2f, 6f)
        audioButton.addListener(
            object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    if (!audioButton.isDisabled) onToggleMusic()
                }
            },
        )
        stage.addActor(audioButton)
    }

    fun update(model: HudModel) {
        location.setText(model.locationLine)
        objective.setText(model.objectiveLine)
        objective.isVisible = model.objectiveLine.isNotBlank()
        signal.setText(model.signalLine)
        signal.isVisible = model.signalLine.isNotBlank()
        prompt.setText(model.promptLine)
        prompt.isVisible = model.promptLine.isNotBlank()
        debug.setText(model.debugText)
        debugMap.setText(model.debugMap)
        audioButton.setText(HudText.music(model.musicAvailable, model.musicMuted))
        audioButton.isDisabled = !model.musicAvailable
        location.pack()
        objective.pack()
        signal.pack()
        prompt.pack()
        debug.pack()
        debugMap.pack()
        audioButton.pack()
        debug.isVisible = model.debugVisible
        debugMap.isVisible = model.debugVisible && model.debugMap.isNotBlank()
        layout()
    }

    fun render(delta: Float) {
        stage.act(delta.coerceAtMost(1f / 15f))
        stage.draw()
    }

    fun resize(width: Int, height: Int, viewport: AsciiViewport) {
        screenWidth = width
        screenHeight = height
        asciiViewport = viewport
        applyGlyphScale(viewport.glyphScale)
        stage.viewport.update(width, height, true)
        layout()
    }

    private fun layout() {
        val viewport = asciiViewport ?: return
        val cellWidth = viewport.scaledCellWidth
        val cellHeight = viewport.scaledCellHeight
        val inset = 2 * viewport.glyphScale
        val left = (viewport.gutterX + cellWidth).toFloat()
        val top = (screenHeight - viewport.gutterY - cellHeight - inset).toFloat()
        location.setPosition(left, top)
        objective.setPosition(left, top - cellHeight)
        signal.setPosition(left, top - cellHeight * 2)
        controls.setPosition(left, (viewport.gutterY + inset).toFloat())
        audioButton.setPosition(
            (screenWidth - viewport.gutterX - audioButton.width - cellWidth).coerceAtLeast(left),
            (viewport.gutterY + inset).toFloat(),
        )

        val centerColumn = viewport.columns / 2
        val centerRow = viewport.rows / 2
        val centerX = (viewport.gutterX + centerColumn * cellWidth).toFloat()
        val centerY = (viewport.gutterY + centerRow * cellHeight).toFloat()
        reticle.setPosition(centerX, centerY)
        prompt.setPosition(
            (screenWidth - prompt.width) * 0.5f,
            centerY - cellHeight * 2,
        )

        debug.setPosition(left, top - debug.height - cellHeight * 2)
        debugMap.setPosition(
            (screenWidth - viewport.gutterX - debugMap.width - cellWidth).coerceAtLeast(left),
            top - debugMap.height + cellHeight,
        )
    }

    private fun applyGlyphScale(nextScale: Int) {
        if (glyphScale == nextScale) return
        glyphScale = nextScale
        val fontScale = nextScale.toFloat()
        arrayOf(location, objective, signal, controls, reticle, prompt, debug, debugMap).forEach {
            it.setFontScale(fontScale)
            it.pack()
        }
        audioButton.label.setFontScale(fontScale)
        audioButton.pad(2f * fontScale, 6f * fontScale, 2f * fontScale, 6f * fontScale)
        audioButton.pack()
    }

    override fun dispose() {
        stage.dispose()
        panelTexture.dispose()
    }

    private fun color(rgba: Int): Color = Color().also { Color.rgba8888ToColor(it, rgba) }

    private fun createPanelTexture(): Texture {
        val pixel = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixel.setColor(0.01f, 0.015f, 0.03f, 0.82f)
        pixel.fill()
        return Texture(pixel).also { pixel.dispose() }
    }
}
