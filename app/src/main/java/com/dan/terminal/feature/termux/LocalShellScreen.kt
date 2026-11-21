package com.dan.terminal.feature.termux

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.IBinder
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontFamily
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient

@Composable
fun LocalShellScreen(
    onSettings: () -> Unit,
    onLegacyConsole: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val appearance = remember(context) { context.getSharedPreferences("dan_terminal_appearance", 0) }
    var fontSizeSp by remember(appearance) {
        mutableIntStateOf(
            ((appearance.all["font_size_sp"] as? Int) ?: DEFAULT_TERMINAL_FONT_SP)
                .coerceIn(MIN_TERMINAL_FONT_SP, MAX_TERMINAL_FONT_SP)
        )
    }
    val terminalTextSize = terminalFontSizePixels(fontSizeSp, LocalDensity.current)
    var blackArchMessage by remember { mutableStateOf<String?>(null) }
    fun changeFontSize(delta: Int) {
        fontSizeSp = (fontSizeSp + delta).coerceIn(MIN_TERMINAL_FONT_SP, MAX_TERMINAL_FONT_SP)
        appearance.edit().putInt("font_size_sp", fontSizeSp).apply()
    }
    var binder by remember { mutableStateOf<PtyTerminalService.LocalBinder?>(null) }
    var serviceError by remember { mutableStateOf<String?>(null) }
    val connection = remember(context) {
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                binder = service as? PtyTerminalService.LocalBinder
                serviceError = if (binder == null) "PTY service returned an unexpected binder." else null
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                binder = null
                serviceError = "PTY service disconnected. Reopen the terminal to start a new session."
            }
        }
    }

    DisposableEffect(context, connection) {
        val intent = Intent(context, PtyTerminalService::class.java)
        var isBound = false
        try {
            context.startService(intent)
            isBound = context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            if (!isBound) serviceError = "Unable to connect to the app-owned PTY service."
        } catch (error: Exception) {
            serviceError = "Unable to start PTY service: ${error.message ?: "unknown error"}"
        }
        onDispose {
            if (isBound) context.unbindService(connection)
        }
    }

    val revision = binder?.screenUpdates?.collectAsState()?.value
    val pendingPaste = binder?.pendingClipboardPaste?.collectAsState()?.value
    val session = binder?.sessionOrNull()
    Column(Modifier.fillMaxSize().imePadding().padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("DAN Terminal", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
            TextButton(onClick = onSettings) { Text("Setup") }
            TextButton(onClick = onLegacyConsole) { Text("Termux") }
            TextButton(onClick = { blackArchMessage = BlackArchSshLauncher.launch(context) }) {
                Text("BlackArch")
            }
        }
        if (blackArchMessage != null) {
            Text(blackArchMessage!!, style = MaterialTheme.typography.bodySmall)
        }
        Text(
            "App sandbox · PTY session · Android UID",
            style = MaterialTheme.typography.labelSmall
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text("Text size", style = MaterialTheme.typography.labelMedium)
            TextButton(
                onClick = { changeFontSize(-2) },
                enabled = fontSizeSp > MIN_TERMINAL_FONT_SP,
                modifier = Modifier.semantics { contentDescription = "Decrease terminal text size" }
            ) { Text("A−") }
            Text(fontSizeSp.toString(), style = MaterialTheme.typography.labelMedium)
            TextButton(
                onClick = { changeFontSize(2) },
                enabled = fontSizeSp < MAX_TERMINAL_FONT_SP,
                modifier = Modifier.semantics { contentDescription = "Increase terminal text size" }
            ) { Text("A+") }
        }
        if (session != null) {
            if (!session.isRunning) {
                Row {
                    Text("Shell exited (status ${session.exitStatus}).")
                    TextButton(onClick = { binder?.restartExitedSession() }) { Text("Start new shell") }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TerminalInputKey.entries.forEach { key ->
                    TextButton(
                        onClick = {
                            val bytes = key.bytes(session.emulator.isCursorKeysApplicationMode)
                            session.write(bytes, 0, bytes.size)
                        },
                        enabled = session.isRunning
                    ) { Text(key.label) }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp)) {
            when {
                serviceError != null -> Text(serviceError!!, color = MaterialTheme.colorScheme.error)
                session == null -> Text("Starting interactive shell…")
                else -> AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                        TerminalView(viewContext, null).apply {
                            setBackgroundColor(Color.rgb(16, 19, 24))
                            setTextSize(terminalTextSize)
                            tag = terminalTextSize
                            contentDescription = "Interactive terminal"
                            isFocusableInTouchMode = true
                            setTerminalViewClient(PtyTerminalViewClient(this))
                            attachSession(session)
                            requestFocus()
                        }
                    },
                    update = { view ->
                        // PTY output also triggers this callback. Only rebuild the
                        // renderer and resize the grid when the font actually changes.
                        if (view.tag != terminalTextSize) {
                            view.setTextSize(terminalTextSize)
                            view.tag = terminalTextSize
                            view.invalidate()
                        }
                        view.attachSession(session)
                        if (revision != null) view.onScreenUpdated()
                    }
                )
            }
        }
    }
    if (pendingPaste != null) {
        AlertDialog(
            onDismissRequest = { binder?.cancelClipboardPaste() },
            title = { Text("Paste multiple lines?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Newlines may submit commands to the shell. Review the text before pasting.")
                    Text(
                        pendingPaste,
                        modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { binder?.confirmClipboardPaste() }) { Text("Paste text") }
            },
            dismissButton = {
                TextButton(onClick = { binder?.cancelClipboardPaste() }) { Text("Cancel paste") }
            }
        )
    }
}

private fun PtyTerminalService.LocalBinder.sessionOrNull(): TerminalSession? =
    runCatching { session }.getOrNull()

private class PtyTerminalViewClient(private val view: TerminalView) : TerminalViewClient {
    override fun onScale(scale: Float): Float = scale
    override fun onSingleTapUp(e: MotionEvent) {
        view.requestFocus()
        (view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
    override fun shouldBackButtonBeMappedToEscape() = false
    override fun shouldEnforceCharBasedInput() = false
    override fun shouldUseCtrlSpaceWorkaround() = false
    override fun isTerminalViewSelected() = true
    override fun copyModeChanged(copyMode: Boolean) = Unit
    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession) = false
    override fun onKeyUp(keyCode: Int, e: KeyEvent) = false
    override fun onLongPress(event: MotionEvent) = false
    override fun readControlKey() = false
    override fun readAltKey() = false
    override fun readShiftKey() = false
    override fun readFnKey() = false
    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession) = false
    override fun onEmulatorSet() = Unit
    override fun logError(tag: String, message: String) = Unit
    override fun logWarn(tag: String, message: String) = Unit
    override fun logInfo(tag: String, message: String) = Unit
    override fun logDebug(tag: String, message: String) = Unit
    override fun logVerbose(tag: String, message: String) = Unit
    override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) = Unit
    override fun logStackTrace(tag: String, e: Exception) = Unit
}
