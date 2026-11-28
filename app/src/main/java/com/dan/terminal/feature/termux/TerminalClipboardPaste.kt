package com.dan.terminal.feature.termux

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory paste intent. Newlines need approval because a shell may execute them. */
class TerminalClipboardPaste(private val paste: (String) -> Unit) {
    private val pending = MutableStateFlow<String?>(null)
    val pendingText: StateFlow<String?> = pending.asStateFlow()

    fun request(text: String) {
        pending.value = null
        if (text.isEmpty()) return
        if ('\n' in text || '\r' in text) pending.value = text else paste(text)
    }

    fun confirm() {
        val text = pending.value ?: return
        pending.value = null
        paste(text)
    }

    fun cancel() { pending.value = null }
}
