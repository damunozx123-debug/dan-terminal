package com.dan.terminal

import com.dan.terminal.navigation.terminalStartRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalStartRouteTest {
    @Test fun incompleteSetupStartsAtSetupAndCompletedSetupStartsAtTerminal() {
        assertEquals("home", terminalStartRoute(false))
        assertEquals("shell", terminalStartRoute(true))
    }
}
