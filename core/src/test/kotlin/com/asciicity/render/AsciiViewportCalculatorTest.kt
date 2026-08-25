package com.asciicity.render

import kotlin.test.Test
import kotlin.test.assertEquals

class AsciiViewportCalculatorTest {
    @Test
    fun `minimum and reference windows select exact one-times grids`() {
        assertEquals(
            AsciiViewport(96, 30, 1, 960, 540, 0, 0),
            AsciiViewportCalculator.calculate(960, 540),
        )
        assertEquals(
            AsciiViewport(128, 40, 1, 1280, 720, 0, 0),
            AsciiViewportCalculator.calculate(1280, 720),
        )
    }

    @Test
    fun `dense profile exactly fills desktop minimum and retains defensive minimum`() {
        assertEquals(
            AsciiViewport(120, 36, 1, 960, 540, 0, 0, AsciiFontProfile.DENSE),
            AsciiViewportCalculator.calculate(960, 540, profile = AsciiFontProfile.DENSE),
        )
        assertEquals(
            AsciiViewport(160, 48, 1, 1280, 720, 0, 0, AsciiFontProfile.DENSE),
            AsciiViewportCalculator.calculate(1280, 720, profile = AsciiFontProfile.DENSE),
        )
    }

    @Test
    fun `dense profile stays logical and uses uniform whole-number scaling`() {
        val reference = AsciiViewportCalculator.calculate(1280, 720, profile = AsciiFontProfile.DENSE)
        assertEquals(
            reference,
            AsciiViewportCalculator.calculate(1280, 720, 2560, 1440, AsciiFontProfile.DENSE),
        )
        assertEquals(
            AsciiViewport(215, 48, 2, 3440, 1440, 0, 0, AsciiFontProfile.DENSE),
            AsciiViewportCalculator.calculate(3440, 1440, profile = AsciiFontProfile.DENSE),
        )
    }

    @Test
    fun `fine profile provides a substantially higher detail surface`() {
        assertEquals(
            AsciiViewport(160, 45, 1, 960, 540, 0, 0, AsciiFontProfile.FINE),
            AsciiViewportCalculator.calculate(960, 540, profile = AsciiFontProfile.FINE),
        )
        assertEquals(
            AsciiViewport(213, 60, 1, 1278, 720, 1, 0, AsciiFontProfile.FINE),
            AsciiViewportCalculator.calculate(1280, 720, profile = AsciiFontProfile.FINE),
        )
    }

    @Test
    fun `non-divisible logical size centers whole-cell surface in gutters`() {
        assertEquals(
            AsciiViewport(100, 33, 1, 1000, 594, 0, 3),
            AsciiViewportCalculator.calculate(1001, 601),
        )
    }

    @Test
    fun `drawable backing pixels never increase logical grid`() {
        assertEquals(
            AsciiViewportCalculator.calculate(1280, 720),
            AsciiViewportCalculator.calculate(1280, 720, 2560, 1440),
        )
    }

    @Test
    fun `below-minimum logical sizes still clamp defensively`() {
        assertEquals(AsciiViewport(96, 30, 1, 960, 540, 0, 0), AsciiViewportCalculator.calculate(800, 450))
    }

    @Test
    fun `larger windows expand the ascii surface without an upper cap`() {
        assertEquals(
            AsciiViewport(160, 50, 1, 1600, 900, 0, 0),
            AsciiViewportCalculator.calculate(1600, 900),
        )
        assertEquals(
            AsciiViewport(256, 60, 1, 2560, 1080, 0, 0),
            AsciiViewportCalculator.calculate(2560, 1080),
        )
    }

    @Test
    fun `ultrawide and four-k windows choose crisp whole-number glyph scales`() {
        assertEquals(
            AsciiViewport(172, 40, 2, 3440, 1440, 0, 0),
            AsciiViewportCalculator.calculate(3440, 1440),
        )
        assertEquals(
            AsciiViewport(128, 40, 3, 3840, 2160, 0, 0),
            AsciiViewportCalculator.calculate(3840, 2160),
        )
    }

    @Test
    fun `ultrawide work area keeps two-times scale below full monitor height`() {
        assertEquals(
            AsciiViewport(172, 39, 2, 3440, 1404, 0, 5),
            AsciiViewportCalculator.calculate(3440, 1415),
        )
    }

    @Test
    fun `work area tolerance never sacrifices the minimum ascii surface`() {
        assertEquals(
            AsciiViewport(121, 38, 1, 1210, 684, 3, 0),
            AsciiViewportCalculator.calculate(1216, 684),
        )
    }

    @Test
    fun `scaled non-divisible windows keep whole glyphs and centered gutters`() {
        assertEquals(
            AsciiViewport(175, 41, 2, 3500, 1476, 0, 12),
            AsciiViewportCalculator.calculate(3500, 1500),
        )
    }
}
