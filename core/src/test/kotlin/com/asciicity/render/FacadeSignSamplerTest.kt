package com.asciicity.render

import com.asciicity.TestContent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class FacadeSignSamplerTest {
    private val materialSampler = MaterialSampler(TestContent.loaded.palette)

    @Test
    fun `samples near and distant layouts with readable face orientation`() {
        val south = sign(
            id = "sign.south",
            face = FacadeFace.SOUTH,
            glyphRows = listOf("ABCD", "1234"),
            distantGlyphRows = listOf("WXYZ", "5678"),
        )
        val north = sign(
            id = "sign.north",
            face = FacadeFace.NORTH,
            cellZ = 2,
            glyphRows = listOf("ABCD", "1234"),
            distantGlyphRows = listOf("WXYZ", "5678"),
        )
        val sampler = FacadeSignSampler(listOf(south, north), materialSampler, 5, 5)
        val output = FacadeSignSample()

        assertTrue(sample(sampler, output, south, FacadeFace.SOUTH, cellX = 1, faceCoordinate = 0.1, depth = 5.0))
        assertEquals('A'.code, output.glyph)
        assertTrue(sample(sampler, output, south, FacadeFace.SOUTH, cellX = 2, faceCoordinate = 0.75, depth = 20.0))
        assertEquals('Z'.code, output.glyph)

        assertTrue(sample(sampler, output, north, FacadeFace.NORTH, cellX = 1, faceCoordinate = 0.1, depth = 5.0))
        assertEquals('D'.code, output.glyph)
        assertTrue(sample(sampler, output, north, FacadeFace.NORTH, cellX = 2, faceCoordinate = 0.75, depth = 5.0))
        assertEquals('A'.code, output.glyph)
    }

    @Test
    fun `animates only authored bulbs and rejects other faces and heights`() {
        val sign = sign(
            id = "sign.bulbs",
            face = FacadeFace.SOUTH,
            glyphRows = listOf("....", "TEXT"),
            distantGlyphRows = listOf("....", "TEXT"),
            animationRate = 3.0,
        )
        val sampler = FacadeSignSampler(listOf(sign), materialSampler, 5, 5)
        val output = FacadeSignSample()

        assertTrue(sample(sampler, output, sign, FacadeFace.SOUTH, cellX = 1, faceCoordinate = 0.1, time = 0.0))
        assertEquals('o'.code, output.glyph)
        assertTrue(sample(sampler, output, sign, FacadeFace.SOUTH, cellX = 1, faceCoordinate = 0.1, time = 0.34))
        assertEquals('.'.code, output.glyph)
        assertFalse(sample(sampler, output, sign, FacadeFace.NORTH, cellX = 1, faceCoordinate = 0.1))
        assertFalse(
            sampler.sample(
                cellX = 1,
                cellZ = 1,
                face = FacadeFace.SOUTH,
                side = RayHitSide.Z_FACE,
                faceCoordinate = 0.1,
                vertical = 0.5,
                depth = 5.0,
                timeSeconds = 0.0,
                output = output,
            ),
        )
    }

    @Test
    fun `ray hit face resolution follows the approached wall side`() {
        assertEquals(FacadeFace.WEST, FacadeFace.fromRayHit(RayHitSide.X_FACE, 1.0, 0.0))
        assertEquals(FacadeFace.EAST, FacadeFace.fromRayHit(RayHitSide.X_FACE, -1.0, 0.0))
        assertEquals(FacadeFace.SOUTH, FacadeFace.fromRayHit(RayHitSide.Z_FACE, 0.0, 1.0))
        assertEquals(FacadeFace.NORTH, FacadeFace.fromRayHit(RayHitSide.Z_FACE, 0.0, -1.0))
        assertEquals(null, FacadeFace.fromRayHit(RayHitSide.CORNER, 1.0, 1.0))
    }

    @Test
    fun `entrance overlays share a facade with signs and keep their lighting contained`() {
        val entrance = sign(
            id = "entrance.club",
            face = FacadeFace.SOUTH,
            glyphRows = listOf("@@@", "@@@"),
            distantGlyphRows = listOf("@@@", "@@@"),
            animationRate = 2.0,
            bottom = 0.0,
            height = 1.8,
            lighting = FacadeLighting.CLUB_STROBE,
        )
        val sign = sign(
            id = "sign.upper",
            face = FacadeFace.SOUTH,
            glyphRows = listOf("SSS", "SSS"),
            distantGlyphRows = listOf("SSS", "SSS"),
            bottom = 2.0,
            height = 1.0,
        )
        val sampler = FacadeSignSampler(listOf(entrance, sign), materialSampler, 5, 5)
        val output = FacadeSignSample()

        assertTrue(sample(sampler, output, entrance, FacadeFace.SOUTH, cellX = 1, faceCoordinate = 0.1, vertical = 1.0))
        assertEquals('@'.code, output.glyph)
        val firstClubBackground = output.background
        assertTrue(sample(sampler, output, entrance, FacadeFace.SOUTH, cellX = 1, faceCoordinate = 0.1, time = 0.5, vertical = 1.0))
        assertNotEquals(firstClubBackground, output.background)

        assertTrue(sample(sampler, output, sign, FacadeFace.SOUTH, cellX = 1, faceCoordinate = 0.1, vertical = 2.5))
        assertEquals('S'.code, output.glyph)
    }

    private fun sample(
        sampler: FacadeSignSampler,
        output: FacadeSignSample,
        sign: FacadeSign,
        face: FacadeFace,
        cellX: Int,
        faceCoordinate: Double,
        depth: Double = 5.0,
        time: Double = 0.0,
        vertical: Double = 2.5,
    ): Boolean = sampler.sample(
        cellX = cellX,
        cellZ = sign.cellZ,
        face = face,
        side = RayHitSide.Z_FACE,
        faceCoordinate = faceCoordinate,
        vertical = vertical,
        depth = depth,
        timeSeconds = time,
        output = output,
    )

    private fun sign(
        id: String,
        face: FacadeFace,
        cellZ: Int = 1,
        glyphRows: List<String>,
        distantGlyphRows: List<String>,
        animationRate: Double = 0.0,
        bottom: Double = 1.0,
        height: Double = 2.0,
        lighting: FacadeLighting = FacadeLighting.MARQUEE,
    ) = FacadeSign(
        id = id,
        face = face,
        cellX = 1,
        cellZ = cellZ,
        widthCells = 2,
        bottom = bottom,
        height = height,
        glyphRows = glyphRows,
        distantGlyphRows = distantGlyphRows,
        borderPaletteKey = PaletteKeys.SIGN_GOLD,
        textPaletteKey = PaletteKeys.SIGN_CREAM,
        accentPaletteKey = PaletteKeys.SIGN_RED_ORANGE,
        backgroundPaletteKey = PaletteKeys.SIGN_BACKGROUND,
        animationRate = animationRate,
        lighting = lighting,
    )
}
