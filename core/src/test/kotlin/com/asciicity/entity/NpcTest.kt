package com.asciicity.entity

import com.asciicity.world.EntityId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NpcTest {
    @Test
    fun `stationary npc metadata retains stable content references`() {
        val npc = Npc(
            entityId = EntityId("npc.rin"),
            spriteId = "sprite.rin",
            facingDegrees = 270.0,
            displayName = "Rin",
            dialogueId = "dialogue.rin.intro",
        )

        assertEquals(EntityId("npc.rin"), npc.entityId)
        assertEquals("sprite.rin", npc.spriteId)
        assertEquals(270.0, npc.facingDegrees)
        assertEquals("Rin", npc.displayName)
        assertEquals("dialogue.rin.intro", npc.dialogueId)
    }

    @Test
    fun `stationary npc metadata rejects invalid facing and blank references`() {
        assertFailsWith<IllegalArgumentException> { npc(facingDegrees = -0.1) }
        assertFailsWith<IllegalArgumentException> { npc(facingDegrees = 360.0) }
        assertFailsWith<IllegalArgumentException> { npc(spriteId = " ") }
        assertFailsWith<IllegalArgumentException> { npc(displayName = " ") }
        assertFailsWith<IllegalArgumentException> { npc(dialogueId = " ") }
    }

    private fun npc(
        spriteId: String = "sprite.rin",
        facingDegrees: Double = 270.0,
        displayName: String = "Rin",
        dialogueId: String = "dialogue.rin.intro",
    ) = Npc(
        entityId = EntityId("npc.rin"),
        spriteId = spriteId,
        facingDegrees = facingDegrees,
        displayName = displayName,
        dialogueId = dialogueId,
    )
}
