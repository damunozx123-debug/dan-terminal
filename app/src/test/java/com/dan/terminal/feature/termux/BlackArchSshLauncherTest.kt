package com.dan.terminal.feature.termux

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class BlackArchSshLauncherTest {
    @Test
    fun launchesNamedSshConfigHostAndPersistentTmuxSession() {
        assertArrayEquals(
            arrayOf("-tt", "blackarch", "tmux", "new-session", "-A", "-s", "dan-phone"),
            BlackArchSshLauncher.arguments
        )
    }
}
