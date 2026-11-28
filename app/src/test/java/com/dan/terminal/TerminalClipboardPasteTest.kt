package com.dan.terminal

import com.dan.terminal.feature.termux.TerminalClipboardPaste
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TerminalClipboardPasteTest {
    @Test fun singleLinePreservesUnicodeAndNeverAddsEnter() {
        val sent = mutableListOf<String>()
        val paste = TerminalClipboardPaste { sent += it }
        paste.request("Māori ✓ 中文")
        assertEquals(listOf("Māori ✓ 中文"), sent)
        assertNull(paste.pendingText.value)
    }

    @Test fun multilineWaitsForConfirmationAndOnlyPastesOnce() {
        val sent = mutableListOf<String>()
        val paste = TerminalClipboardPaste { sent += it }
        val text = "printf first\nprintf second\n"
        paste.request(text)
        assertEquals(emptyList<String>(), sent)
        assertEquals(text, paste.pendingText.value)
        paste.confirm()
        paste.confirm()
        assertEquals(listOf(text), sent)
        assertNull(paste.pendingText.value)
    }

    @Test fun cancellationDiscardsPendingCommands() {
        val sent = mutableListOf<String>()
        val paste = TerminalClipboardPaste { sent += it }
        paste.request("first\nsecond")
        paste.cancel()
        paste.confirm()
        assertEquals(emptyList<String>(), sent)
        assertNull(paste.pendingText.value)
    }

    @Test fun carriageReturnsAlsoRequireConfirmation() {
        val sent = mutableListOf<String>()
        val paste = TerminalClipboardPaste { sent += it }
        for (text in listOf("first\rsecond", "first\r\nsecond")) {
            paste.request(text)
            assertEquals(text, paste.pendingText.value)
            paste.cancel()
        }
        assertEquals(emptyList<String>(), sent)
    }

    @Test fun newRequestReplacesPendingClipboardText() {
        val sent = mutableListOf<String>()
        val paste = TerminalClipboardPaste { sent += it }
        paste.request("old\ncommand")
        paste.request("new\ncommand")
        paste.confirm()
        assertEquals(listOf("new\ncommand"), sent)
    }

    @Test fun emptyClipboardNeverSubmitsAnything() {
        val sent = mutableListOf<String>()
        val paste = TerminalClipboardPaste { sent += it }
        paste.request("")
        paste.confirm()
        assertEquals(emptyList<String>(), sent)
    }
}
