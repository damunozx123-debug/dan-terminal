package com.dan.terminal.feature.termux

import androidx.lifecycle.ViewModel

/**
 * Owns the shell independently of Compose screen recreation.
 * Navigation away from the shell does not terminate the process.
 * Android process death still terminates this prototype; a foreground
 * session service and a real PTY are separate M3 requirements.
 */
class LocalShellViewModel : ViewModel() {
    val session = LocalShellSession().also { it.start() }

    override fun onCleared() {
        session.dispose()
        super.onCleared()
    }
}
