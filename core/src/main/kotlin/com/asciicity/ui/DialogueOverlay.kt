package com.asciicity.ui

import com.asciicity.content.DialogueDefinition
import com.asciicity.render.PaletteKeys
import com.asciicity.render.RenderPalette
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.ScreenViewport

internal data class DialogueLine(
    val speaker: String,
    val text: String,
    val number: Int,
    val total: Int,
)

internal class DialogueSession {
    private var speaker = ""
    private var lines: List<String> = emptyList()
    private var lineIndex = 0

    val currentLine: DialogueLine?
        get() = lines.getOrNull(lineIndex)?.let { text ->
            DialogueLine(
                speaker = speaker,
                text = text,
                number = lineIndex + 1,
                total = lines.size,
            )
        }

    val isOpen: Boolean
        get() = lineIndex in lines.indices

    fun open(dialogue: DialogueDefinition) {
        open(dialogue.speaker, dialogue.lines)
    }

    fun open(speaker: String, lines: List<String>) {
        require(speaker.isNotBlank()) { "Dialogue speaker must not be blank" }
        require(lines.isNotEmpty() && lines.none(String::isBlank)) {
            "Dialogue must contain non-blank lines"
        }
        this.speaker = speaker
        this.lines = lines.toList()
        lineIndex = 0
    }

    fun advance(): Boolean {
        if (!isOpen) return false
        lineIndex += 1
        if (lineIndex < lines.size) return true
        close()
        return false
    }

    fun close() {
        speaker = ""
        lines = emptyList()
        lineIndex = 0
    }
}

class DialogueOverlay(
    font: BitmapFont,
    palette: RenderPalette,
) : Disposable {
    private val stage = Stage(ScreenViewport())
    private val panelTexture: Texture
    private val session = DialogueSession()
    private val root = Table()
    private val panel = Table()
    private val speaker: Label
    private val message: Label
    private val progress: Label
    private val hint: Label
    private val messageCell: Cell<Label>
    private val progressCell: Cell<Label>
    private val hintCell: Cell<Label>
    private val panelCell: Cell<Table>
    private var glyphScale = 1

    val isVisible: Boolean
        get() = session.isOpen

    init {
        val pixel = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixel.setColor(0.015f, 0.02f, 0.04f, 0.94f)
        pixel.fill()
        panelTexture = Texture(pixel)
        pixel.dispose()

        val speakerStyle = Label.LabelStyle(font, color(palette[PaletteKeys.NEON_CYAN]))
        val textStyle = Label.LabelStyle(font, color(palette[PaletteKeys.HUD]))
        speaker = Label("", speakerStyle)
        message = Label("", textStyle)
        message.setAlignment(Align.center)
        message.setWrap(true)
        progress = Label("", textStyle)
        hint = Label("", textStyle)

        panel.background = TextureRegionDrawable(TextureRegion(panelTexture))
        panel.pad(28f)
        panel.add(speaker).center().row()
        messageCell = panel.add(message).width(620f).padTop(22f).center()
        panel.row()
        progressCell = panel.add(progress).padTop(18f).center()
        panel.row()
        hintCell = panel.add(hint).padTop(10f).center()

        root.setFillParent(true)
        panelCell = root.add(panel).width(700f)
        root.isVisible = false
        stage.addActor(root)
    }

    fun show(dialogue: DialogueDefinition) {
        session.open(dialogue)
        refresh()
    }

    fun show(speaker: String, lines: List<String>) {
        session.open(speaker, lines)
        refresh()
    }

    fun advance(): Boolean {
        val remainsVisible = session.advance()
        if (remainsVisible) refresh() else root.isVisible = false
        return remainsVisible
    }

    fun hide() {
        session.close()
        root.isVisible = false
    }

    fun render(delta: Float) {
        if (!isVisible) return
        stage.act(delta.coerceAtMost(1f / 15f))
        stage.draw()
    }

    fun resize(width: Int, height: Int, nextGlyphScale: Int) {
        applyGlyphScale(nextGlyphScale)
        stage.viewport.update(width, height, true)
    }

    private fun refresh() {
        val line = checkNotNull(session.currentLine)
        speaker.setText(line.speaker)
        message.setText(line.text)
        progress.setText("${line.number} / ${line.total}")
        hint.setText(if (line.number < line.total) "E / Enter  NEXT   |   Esc  CLOSE" else "E / Enter / Esc  CLOSE")
        root.isVisible = true
        root.invalidateHierarchy()
    }

    private fun applyGlyphScale(nextScale: Int) {
        if (glyphScale == nextScale) return
        glyphScale = nextScale
        val scale = nextScale.toFloat()
        speaker.setFontScale(scale)
        message.setFontScale(scale)
        progress.setFontScale(scale)
        hint.setFontScale(scale)
        panel.pad(28f * scale)
        messageCell.width(620f * scale).padTop(22f * scale)
        progressCell.padTop(18f * scale)
        hintCell.padTop(10f * scale)
        panelCell.width(700f * scale)
        root.invalidateHierarchy()
    }

    override fun dispose() {
        stage.dispose()
        panelTexture.dispose()
    }

    private fun color(rgba: Int): Color = Color().also { Color.rgba8888ToColor(it, rgba) }
}
