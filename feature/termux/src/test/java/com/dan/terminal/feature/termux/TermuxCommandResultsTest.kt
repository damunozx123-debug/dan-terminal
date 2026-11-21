package com.dan.terminal.feature.termux

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TermuxCommandResultsTest {
    @Test fun commandLifecycle() {
        TermuxCommandResults.clear()
        val id = TermuxCommandResults.start("uname -a")
        assertNotNull(id)
        assertNull(TermuxCommandResults.start("pwd"))
        TermuxCommandResults.finish(id!!, "Linux", "", 0)
        assertEquals(CommandState.FINISHED, TermuxCommandResults.entries.value.single().state)
        assertEquals("Linux", TermuxCommandResults.entries.value.single().stdout)
        assertNotNull(TermuxCommandResults.start("pwd"))
        TermuxCommandResults.clear()
    }

    @Test fun failureAndLateCallbackAfterClear() {
        TermuxCommandResults.clear()
        val id = TermuxCommandResults.start("false")!!
        TermuxCommandResults.finish(id, "", "failed", 1)
        assertEquals(CommandState.FAILED, TermuxCommandResults.entries.value.single().state)
        TermuxCommandResults.clear()
        TermuxCommandResults.finish(id, "late", "", 0)
        assertEquals(0, TermuxCommandResults.entries.value.size)
    }

    @Test fun rejectBlankAndOversizedCommands() {
        TermuxCommandResults.clear()
        assertNull(TermuxCommandResults.start("  "))
        assertNull(TermuxCommandResults.start("x".repeat(4097)))
        assertFalse(TermuxCommandResults.entries.value.isNotEmpty())
    }
}
