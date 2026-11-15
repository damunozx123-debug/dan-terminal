package com.dan.terminal.feature.termux

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns the app-sandbox shell and its PTY independently of the Compose screen.
 * Navigation retains the started service. Android may stop an unbound background
 * service or kill the process; this prototype does not guarantee background survival.
 */
class PtyTerminalService : Service() {
    inner class LocalBinder : Binder() {
        val session: TerminalSession
            get() = checkNotNull(terminalSession) { "PTY session is not initialized" }
        val screenUpdates: StateFlow<Long> get() = updates.asStateFlow()
        val pendingClipboardPaste: StateFlow<String?> get() = clipboardPaste.pendingText
        fun confirmClipboardPaste() = clipboardPaste.confirm()
        fun cancelClipboardPaste() = clipboardPaste.cancel()
        fun restartExitedSession() {
            check(terminalSession?.isRunning != true) { "The shell is still running" }
            clipboardPaste.cancel()
            terminalSession = createSession()
            updates.value += 1
        }
    }

    private val binder = LocalBinder()
    private var terminalSession: TerminalSession? = null
    private val updates = MutableStateFlow(0L)
    private val clipboardPaste = TerminalClipboardPaste { text ->
        terminalSession?.takeIf { it.isRunning }?.emulator?.paste(text)
    }

    override fun onCreate() {
        super.onCreate()
        terminalSession = createSession()
    }

    private fun createSession(): TerminalSession {
        val home = filesDir.absolutePath
        val env = arrayOf(
            "HOME=$home",
            "TMPDIR=${cacheDir.absolutePath}",
            "PATH=/system/bin:/system/xbin",
            "TERM=xterm-256color"
        )
        return TerminalSession(
            "/system/bin/sh",
            home,
            arrayOf("-i"),
            env,
            2000,
            SessionClient
        ).also { it.updateSize(80, 24) }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onDestroy() {
        clipboardPaste.cancel()
        terminalSession?.finishIfRunning()
        terminalSession = null
        super.onDestroy()
    }

    private val SessionClient = object : TerminalSessionClient {
        override fun onTextChanged(changedSession: TerminalSession) { updates.value += 1 }
        override fun onTitleChanged(changedSession: TerminalSession) = Unit
        override fun onSessionFinished(finishedSession: TerminalSession) {
            if (finishedSession === terminalSession) clipboardPaste.cancel()
            updates.value += 1
        }
        override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("DAN Terminal selection", text))
        }
        override fun onPasteTextFromClipboard(session: TerminalSession) {
            if (session !== terminalSession || !session.isRunning) return
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = clipboard.primaryClip ?: return
            if (clip.itemCount == 0) return
            val text = clip.getItemAt(0).coerceToText(this@PtyTerminalService)?.toString().orEmpty()
            clipboardPaste.request(text)
        }
        override fun onBell(session: TerminalSession) = Unit
        override fun onColorsChanged(session: TerminalSession) { updates.value += 1 }
        override fun onTerminalCursorStateChange(state: Boolean) { updates.value += 1 }
        override fun getTerminalCursorStyle(): Int? = null
        override fun logError(tag: String, message: String) = Unit
        override fun logWarn(tag: String, message: String) = Unit
        override fun logInfo(tag: String, message: String) = Unit
        override fun logDebug(tag: String, message: String) = Unit
        override fun logVerbose(tag: String, message: String) = Unit
        override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) = Unit
        override fun logStackTrace(tag: String, e: Exception) = Unit
    }
}
