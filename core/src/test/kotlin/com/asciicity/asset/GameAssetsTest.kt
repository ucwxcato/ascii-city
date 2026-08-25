package com.asciicity.asset

import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration
import com.badlogic.gdx.ApplicationAdapter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GameAssetsTest {
    @Test
    fun `access before load fails and disposal is idempotent`() {
        val assets = GameAssets()

        val error = assertFailsWith<IllegalStateException> { assets.font }
        assertTrue(error.message.orEmpty().contains("NOT_STARTED"))

        assets.dispose()
        assets.dispose()
        assertEquals(AssetLoadState.DISPOSED, assets.state)
    }

    @Test
    fun `missing font and corrupt palette become controlled asset failures`() {
        val application = HeadlessApplication(
            object : ApplicationAdapter() {},
            HeadlessApplicationConfiguration(),
        )
        try {
            val missingFont = GameAssets(AssetPaths(font = "fonts/does-not-exist.fnt"))
            val missingFontError = assertFailsWith<AssetLoadException> { missingFont.loadBlocking() }
            assertTrue(missingFontError.userMessage.contains("does-not-exist.fnt"))
            assertEquals(AssetLoadState.FAILED, missingFont.state)
            missingFont.dispose()

            val missingDenseFont = GameAssets(AssetPaths(denseWorldFont = "fonts/dense-does-not-exist.fnt"))
            val missingDenseFontError = assertFailsWith<AssetLoadException> { missingDenseFont.loadBlocking() }
            assertTrue(missingDenseFontError.userMessage.contains("dense-does-not-exist.fnt"))
            assertEquals(AssetLoadState.FAILED, missingDenseFont.state)
            missingDenseFont.dispose()

            val missingFineFont = GameAssets(AssetPaths(fineWorldFont = "fonts/fine-does-not-exist.fnt"))
            val missingFineFontError = assertFailsWith<AssetLoadException> { missingFineFont.loadBlocking() }
            assertTrue(missingFineFontError.userMessage.contains("fine-does-not-exist.fnt"))
            assertEquals(AssetLoadState.FAILED, missingFineFont.state)
            missingFineFont.dispose()

            val corruptPalette = GameAssets(AssetPaths(palette = "test-fixtures/corrupt-palette.json"))
            val corruptPaletteError = assertFailsWith<AssetLoadException> { corruptPalette.loadBlocking() }
            assertTrue(
                corruptPaletteError.userMessage.contains("corrupt-palette.json"),
                corruptPaletteError.userMessage,
            )
            assertEquals(AssetLoadState.FAILED, corruptPalette.state)
            corruptPalette.dispose()

            val missingPalette = GameAssets(AssetPaths(palette = "data/does-not-exist.json"))
            val missingPaletteError = assertFailsWith<AssetLoadException> { missingPalette.loadBlocking() }
            assertTrue(missingPaletteError.userMessage.contains("does-not-exist.json"))
            assertEquals(AssetLoadState.FAILED, missingPalette.state)
            missingPalette.dispose()
        } finally {
            application.exit()
        }
    }
}
