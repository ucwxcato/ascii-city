package com.asciicity

import com.asciicity.asset.AssetLoadException
import com.asciicity.asset.AssetPaths
import com.asciicity.asset.GameAssets
import com.asciicity.content.ContentLoader
import com.asciicity.content.ContentValidationException
import com.asciicity.content.MapCatalog
import com.asciicity.screen.AssetErrorScreen
import com.asciicity.screen.GameScreen
import com.asciicity.render.AsciiFontProfile
import com.asciicity.util.GameLog
import com.asciicity.world.WorldPosition
import com.badlogic.gdx.Application
import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import java.nio.file.Path

class AsciiCityGame(
    private val exitAfterFrames: Int? = null,
    private val screenshotPath: String? = null,
    private val assetPaths: AssetPaths = AssetPaths(),
    private val loadVisualAssets: Boolean = true,
    private val initialYawDegrees: Double? = null,
    private val initialPitchDegrees: Double? = null,
    private val initialPosition: WorldPosition? = null,
    private val verifyWalk: Boolean = false,
    private val verifyInteraction: Boolean = false,
    private val verifyCasinoEntry: Boolean = false,
    private val verifyCasinoReturn: Boolean = false,
    private val verifyPresentation: Boolean = false,
    private val audioEnabled: Boolean = true,
    private val headBobEnabled: Boolean = true,
    private val initialMapId: String = MapCatalog.DEFAULT_MAP_ID,
    private val screenshotFrame: Int = DEFAULT_SCREENSHOT_FRAME,
    private val worldFontProfile: AsciiFontProfile = AsciiFontProfile.DEFAULT,
) : Game() {
    private lateinit var assets: GameAssets
    private var renderedFrames = 0
    private var screenshotCaptured = false
    private var mapChangeQueued = false

    override fun create() {
        Gdx.app.logLevel = Application.LOG_DEBUG
        GameLog.info(
            "AsciiCityGame",
            "ASCII City started at ${Gdx.graphics.width}x${Gdx.graphics.height} logical pixels",
        )
        if (!loadVisualAssets) {
            return
        }
        assets = GameAssets(assetPaths)
        try {
            assets.loadBlocking()
            showMap(initialMapId, debugVisible = false, initialPosition = initialPosition)
        } catch (error: AssetLoadException) {
            GameLog.error("AsciiCityGame", error.userMessage, error)
            setScreen(AssetErrorScreen(error.userMessage))
        } catch (error: ContentValidationException) {
            val userMessage = "Required city content could not be loaded. ${error.message}"
            GameLog.error("AsciiCityGame", userMessage, error)
            setScreen(AssetErrorScreen(userMessage))
        }
    }

    override fun render() {
        Gdx.gl.glClearColor(0.005f, 0.008f, 0.015f, 1.0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        super.render()

        renderedFrames += 1
        if (!screenshotCaptured && screenshotPath != null && renderedFrames >= screenshotFrame) {
            captureScreenshot(screenshotPath)
            screenshotCaptured = true
        }
        if (exitAfterFrames != null && renderedFrames >= exitAfterFrames) {
            GameLog.info("AsciiCityGame", "Desktop smoke check completed after $renderedFrames frames")
            Gdx.app.exit()
        }
    }

    override fun dispose() {
        GameLog.info("AsciiCityGame", "ASCII City stopped")
        super.dispose()
        if (::assets.isInitialized) {
            assets.dispose()
        }
    }

    private fun showMap(
        mapId: String,
        debugVisible: Boolean,
        initialPosition: WorldPosition? = null,
        initialYawDegrees: Double? = this.initialYawDegrees,
    ) {
        val content = ContentLoader().load(Path.of("."), mapId)
        initialPosition?.let { position ->
            if (!content.world.grid.contains(position.toGridCell()) || content.world.grid.cellAt(position.toGridCell()).solid) {
                throw ContentValidationException("Travel destination for '$mapId' must be on an in-bounds non-solid cell")
            }
        }
        val previousScreen = screen
        val nextScreen = GameScreen(
            assets,
            content,
            initialYawDegrees,
            initialPitchDegrees,
            verifyWalk,
            verifyInteraction,
            verifyCasinoEntry,
            verifyCasinoReturn,
            verifyPresentation,
            audioEnabled,
            headBobEnabled,
            nextMapId = MapCatalog.nextAfter(mapId),
            onMapSwitchRequested = ::queueMapSwitch,
            onMapTravelRequested = ::queueMapTravel,
            initialDebugVisible = debugVisible,
            initialPosition = initialPosition,
            worldFontProfile = worldFontProfile,
        )
        setScreen(nextScreen)
        previousScreen?.dispose()
        GameLog.info("AsciiCityGame", "Loaded playable map '$mapId'")
    }

    private fun queueMapSwitch(mapId: String) {
        queueMapChange(mapId, debugVisible = true, reason = "Debug")
    }

    private fun queueMapTravel(destination: com.asciicity.system.InteractionOutcome.EnterMap) {
        if (mapChangeQueued) return
        mapChangeQueued = true
        Gdx.app.postRunnable {
            try {
                showMap(
                    mapId = destination.mapId,
                    debugVisible = false,
                    initialPosition = destination.arrivalPosition,
                    initialYawDegrees = destination.arrivalYawDegrees,
                )
            } catch (error: ContentValidationException) {
                GameLog.error("AsciiCityGame", "Travel map '${destination.mapId}' could not be loaded. ${error.message}", error)
            } finally {
                mapChangeQueued = false
            }
        }
    }

    private fun queueMapChange(mapId: String, debugVisible: Boolean, reason: String) {
        if (mapChangeQueued) return
        mapChangeQueued = true
        Gdx.app.postRunnable {
            try {
                showMap(mapId, debugVisible = debugVisible)
            } catch (error: ContentValidationException) {
                GameLog.error("AsciiCityGame", "$reason map '$mapId' could not be loaded. ${error.message}", error)
            } finally {
                mapChangeQueued = false
            }
        }
    }

    private fun captureScreenshot(path: String) {
        val pixmap = Pixmap.createFromFrameBuffer(
            0,
            0,
            Gdx.graphics.backBufferWidth,
            Gdx.graphics.backBufferHeight,
        )
        val output = Gdx.files.absolute(path)
        output.parent().mkdirs()
        val encoder = PixmapIO.PNG()
        try {
            encoder.setFlipY(true)
            encoder.write(output, pixmap)
            GameLog.info("AsciiCityGame", "Captured screenshot '${output.path()}'")
        } finally {
            encoder.dispose()
            pixmap.dispose()
        }
    }

    companion object {
        private const val DEFAULT_SCREENSHOT_FRAME = 4
    }
}
