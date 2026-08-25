package com.asciicity.ui

import com.asciicity.content.DialogueDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DialogueSessionTest {
    @Test
    fun `linear dialogue opens at first line advances and closes after final line`() {
        val session = DialogueSession()
        val dialogue = DialogueDefinition(
            id = "dialogue.test",
            speaker = "Rin",
            lines = listOf("First line", "Second line"),
        )

        session.open(dialogue)

        assertTrue(session.isOpen)
        assertEquals(DialogueLine("Rin", "First line", 1, 2), session.currentLine)
        assertTrue(session.advance())
        assertEquals(DialogueLine("Rin", "Second line", 2, 2), session.currentLine)
        assertFalse(session.advance())
        assertFalse(session.isOpen)
        assertNull(session.currentLine)
    }

    @Test
    fun `closing and reopening replace all transient dialogue state`() {
        val session = DialogueSession()
        session.open("TERMINAL", listOf("Old line", "Old final"))
        session.advance()

        session.close()
        session.open("ARCHIVE", listOf("Fresh line"))

        assertEquals(DialogueLine("ARCHIVE", "Fresh line", 1, 1), session.currentLine)
        assertFalse(session.advance())
        assertFalse(session.advance())
    }
}
