package com.asciicity.asset

import com.asciicity.render.AsciiFontProfile
import com.asciicity.render.PaletteValidationException
import com.asciicity.render.RenderPalette
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont

class GameAssets(
    private val paths: AssetPaths = AssetPaths(),
    private val manager: AssetManager = AssetManager(),
) : AutoCloseable {
    private val fontKey = AssetKey(paths.font, BitmapFont::class.java)
    private val denseWorldFontKey = AssetKey(paths.denseWorldFont, BitmapFont::class.java)
    private val fineWorldFontKey = AssetKey(paths.fineWorldFont, BitmapFont::class.java)

    var state: AssetLoadState = AssetLoadState.NOT_STARTED
        private set

    var failure: AssetLoadException? = null
        private set

    lateinit var palette: RenderPalette
        private set

    val font: BitmapFont
        get() = get(fontKey)

    fun worldFont(profile: AsciiFontProfile): BitmapFont = when (profile) {
        AsciiFontProfile.STANDARD -> font
        AsciiFontProfile.DENSE -> get(denseWorldFontKey)
        AsciiFontProfile.FINE -> get(fineWorldFontKey)
    }

    fun loadBlocking() {
        check(state == AssetLoadState.NOT_STARTED) {
            "Assets can only be loaded once; current state is $state"
        }
        state = AssetLoadState.LOADING

        try {
            palette = RenderPalette.load(Gdx.files.internal(paths.palette))
            manager.load(fontKey.path, fontKey.type)
            manager.load(denseWorldFontKey.path, denseWorldFontKey.type)
            manager.load(fineWorldFontKey.path, fineWorldFontKey.type)
            manager.finishLoading()
            configureFont(fontKey, AsciiFontProfile.STANDARD)
            configureFont(denseWorldFontKey, AsciiFontProfile.DENSE)
            configureFont(fineWorldFontKey, AsciiFontProfile.FINE)
            state = AssetLoadState.READY
        } catch (error: Exception) {
            state = AssetLoadState.FAILED
            manager.clear()
            val userMessage = when (error) {
                is PaletteValidationException -> "Required palette '${paths.palette}' is invalid. ${error.message}"
                else -> "Required bitmap world font could not be loaded " +
                    "or did not satisfy its selected ASCII profile " +
                    "(standard='${paths.font}', dense='${paths.denseWorldFont}', fine='${paths.fineWorldFont}'). ${error.message}"
            }
            val wrapped = AssetLoadException(
                userMessage = userMessage,
                cause = error,
            )
            failure = wrapped
            throw wrapped
        }
    }

    private fun configureFont(key: AssetKey<BitmapFont>, profile: AsciiFontProfile) {
        val loadedFont = manager.get(key.path, key.type)
        try {
            profile.validate(loadedFont)
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Bitmap font '${key.path}' does not satisfy the '${profile.id}' world profile. ${error.message}",
                error,
            )
        }
        loadedFont.setUseIntegerPositions(true)
        loadedFont.region.texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }

    fun <T : Any> get(key: AssetKey<T>): T {
        check(state == AssetLoadState.READY) {
            "Asset '${key.path}' is unavailable while asset state is $state"
        }
        return manager.get(key.path, key.type)
    }

    override fun close() {
        dispose()
    }

    fun dispose() {
        if (state == AssetLoadState.DISPOSED) {
            return
        }
        manager.dispose()
        state = AssetLoadState.DISPOSED
    }
}
