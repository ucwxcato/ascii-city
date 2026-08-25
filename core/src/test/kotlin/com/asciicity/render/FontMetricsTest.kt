package com.asciicity.render

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FontMetricsTest {
    @Test
    fun `bundled definition covers printable ascii in fixed cells`() {
        val definitionPath = Path.of("fonts", "vt323-10x18.fnt")
        FontMetrics.validateDefinition(Files.readString(definitionPath))
        AsciiFontProfile.DENSE.validateDefinition(Files.readString(Path.of("fonts", "vt323-8x15.fnt")))
        AsciiFontProfile.FINE.validateDefinition(Files.readString(Path.of("fonts", "vt323-6x12.fnt")))

        assertEquals(1280, FontMetrics.REFERENCE_COLUMNS * FontMetrics.CELL_WIDTH)
        assertEquals(720, FontMetrics.REFERENCE_ROWS * FontMetrics.CELL_HEIGHT)
        assertEquals(960, FontMetrics.MINIMUM_COLUMNS * FontMetrics.CELL_WIDTH)
        assertEquals(540, FontMetrics.MINIMUM_ROWS * FontMetrics.CELL_HEIGHT)
        assertEquals(1280, AsciiFontProfile.DENSE.referenceLogicalWidth)
        assertEquals(720, AsciiFontProfile.DENSE.referenceLogicalHeight)
        assertEquals(960, AsciiFontProfile.DENSE.minimumLogicalWidth)
        assertEquals(540, AsciiFontProfile.DENSE.minimumLogicalHeight)
        assertEquals(1278, AsciiFontProfile.FINE.referenceLogicalWidth)
        assertEquals(720, AsciiFontProfile.FINE.referenceLogicalHeight)
        assertEquals(960, AsciiFontProfile.FINE.minimumLogicalWidth)
        assertEquals(540, AsciiFontProfile.FINE.minimumLogicalHeight)
    }

    @Test
    fun `font source metadata and license ship with runtime files`() {
        val requiredFiles = listOf(
            Path.of("fonts", "vt323-10x18.fnt"),
            Path.of("fonts", "vt323-10x18.png"),
            Path.of("fonts", "vt323-8x15.fnt"),
            Path.of("fonts", "vt323-8x15.png"),
            Path.of("fonts", "vt323-6x12.fnt"),
            Path.of("fonts", "vt323-6x12.png"),
            Path.of("fonts", "source", "VT323-Regular.ttf"),
            Path.of("licenses", "vt323", "OFL.txt"),
            Path.of("licenses", "vt323", "METADATA.pb"),
        )
        assertTrue(requiredFiles.all(Files::isRegularFile))
    }

    @Test
    fun `definition validation rejects incomplete coverage`() {
        val error = assertFailsWith<IllegalArgumentException> {
            FontMetrics.validateDefinition("common lineHeight=18\nchars count=0")
        }
        assertTrue(error.message.orEmpty().contains("printable ASCII"))
    }

    @Test
    fun `profile ids select only supported world surfaces`() {
        assertEquals(AsciiFontProfile.FINE, AsciiFontProfile.DEFAULT)
        assertEquals(AsciiFontProfile.STANDARD, AsciiFontProfile.fromId("standard"))
        assertEquals(AsciiFontProfile.DENSE, AsciiFontProfile.fromId("DENSE"))
        assertEquals(AsciiFontProfile.FINE, AsciiFontProfile.fromId("fine"))
        val error = assertFailsWith<IllegalArgumentException> { AsciiFontProfile.fromId("wide") }
        assertTrue(error.message.orEmpty().contains("--ascii-density"))
    }
}
