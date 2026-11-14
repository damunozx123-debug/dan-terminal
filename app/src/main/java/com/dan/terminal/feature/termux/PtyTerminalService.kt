package com.dan.terminal.feature.termux

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient

/**
 * Owns the app-sandbox shell and its PTY independently of the Compose screen.
 * The session lasts for the service process lifetime; Android process death ends it.
 */
class PtyTerminalService : Service() {
    inner class LocalBinder : Binder() {
        val session: TerminalSession
            get() = checkNotNull(terminalSession) { "PTY session is not initialized" }
    }

    private val binder = LocalBinder()
    private var terminalSession: TerminalSession? = null

    override fun onCreate() {
        super.onCreate()
        val home = filesDir.absolutePath
        val env = arrayOf(
            "HOME=$home",
            "TMPDIR=${cacheDir.absolutePath}",
            "PATH=/system/bin:/system/xbin",
            "TERM=xterm-256color"
        )
        terminalSession = TerminalSession(
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
        terminalSession?.finishIfRunning()
        terminalSession = null
        super.onDestroy()
    }

    private object SessionClient : TerminalSessionClient {
        override fun onTextChanged(changedSession: TerminalSession) = Unit
        override fun onTitleChanged(changedSession: TerminalSession) = Unit
        override fun onSessionFinished(finishedSession: TerminalSession) = Unit
        override fun onCopyTextToClipboard(session: TerminalSession, text: String) = Unit
        override fun onPasteTextFromClipboard(session: TerminalSession) = Unit
        override fun onBell(session: TerminalSession) = Unit
        override fun onColorsChanged(session: TerminalSession) = Unit
        override fun onTerminalCursorStateChange(state: Boolean) = Unit
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
