package com.asciicity.audio

import com.asciicity.content.ContentLoader
import com.asciicity.world.WorldPosition
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WaterfallAudioFieldTest {
    @Test
    fun `distance field is full near its source smooth at midpoint and silent outside`() {
        val field = WaterfallAudioField.fromPositions(
            sources = listOf(WorldPosition(0.5, 0.5)),
            fullVolumeDistance = 2.0,
            silentDistance = 10.0,
        )

        assertEquals(1f, field.mixAt(WorldPosition(0.5, 0.5)))
        assertEquals(1f, field.mixAt(WorldPosition(2.5, 0.5)))
        assertEquals(0.5f, field.mixAt(WorldPosition(6.5, 0.5)))
        assertEquals(0f, field.mixAt(WorldPosition(10.5, 0.5)))
    }

    @Test
    fun `city alpha derives both waterfall cells and keeps the city spawn silent`() {
        val content = ContentLoader().load(Path.of("."), "city-alpha")
        val field = WaterfallAudioField.fromGrid(content.world.grid)

        assertEquals(2, field.sourceCount)
        assertEquals(0f, field.mixAt(content.world.player.position))
        assertEquals(1f, field.mixAt(WorldPosition(2.5, 25.5)))
    }

    @Test
    fun `empty and invalid fields are safe`() {
        val empty = WaterfallAudioField.fromPositions(emptyList())
        assertEquals(0f, empty.mixAt(WorldPosition(0.5, 0.5)))

        assertFailsWith<IllegalArgumentException> {
            WaterfallAudioField.fromPositions(emptyList(), fullVolumeDistance = -1.0)
        }
        assertFailsWith<IllegalArgumentException> {
            WaterfallAudioField.fromPositions(emptyList(), fullVolumeDistance = 2.0, silentDistance = 2.0)
        }
    }
}
