package com.dan.terminal

import com.dan.terminal.feature.termux.TerminalInputKey
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class TerminalInputKeysTest {
    @Test fun phoneControlKeysEmitTerminalControlSequences() {
        assertArrayEquals(byteArrayOf(0x03), TerminalInputKey.CTRL_C.sequence)
        assertArrayEquals(byteArrayOf(0x04), TerminalInputKey.CTRL_D.sequence)
        assertArrayEquals(byteArrayOf(0x1b), TerminalInputKey.ESC.sequence)
        assertArrayEquals(byteArrayOf(0x09), TerminalInputKey.TAB.sequence)
        assertArrayEquals("\u001b[A".toByteArray(), TerminalInputKey.UP.sequence)
        assertArrayEquals("\u001b[B".toByteArray(), TerminalInputKey.DOWN.sequence)
        assertArrayEquals("\u001b[C".toByteArray(), TerminalInputKey.RIGHT.sequence)
        assertArrayEquals("\u001b[D".toByteArray(), TerminalInputKey.LEFT.sequence)
    }

    @Test fun arrowKeysRespectApplicationCursorModeUsedByInteractivePrograms() {
        assertArrayEquals("\u001bOA".toByteArray(), TerminalInputKey.UP.bytes(true))
        assertArrayEquals("\u001bOB".toByteArray(), TerminalInputKey.DOWN.bytes(true))
        assertArrayEquals("\u001bOC".toByteArray(), TerminalInputKey.RIGHT.bytes(true))
        assertArrayEquals("\u001bOD".toByteArray(), TerminalInputKey.LEFT.bytes(true))
        assertArrayEquals(byteArrayOf(3), TerminalInputKey.CTRL_C.bytes(true))
    }
}
