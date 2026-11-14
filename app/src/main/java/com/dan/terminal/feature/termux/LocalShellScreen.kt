package com.dan.terminal.feature.termux

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.os.IBinder
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient

@Composable
fun LocalShellScreen(
    onSettings: () -> Unit,
    onLegacyConsole: () -> Unit
) {
    val context = LocalContext.current.applicationContext
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

    val session = binder?.sessionOrNull()
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("DAN Terminal", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
            TextButton(onClick = onSettings) { Text("Setup") }
            TextButton(onClick = onLegacyConsole) { Text("Termux") }
        }
        Text(
            "App sandbox · PTY session · Android UID",
            style = MaterialTheme.typography.labelSmall
        )
        Box(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp)) {
            when {
                serviceError != null -> Text(serviceError!!, color = MaterialTheme.colorScheme.error)
                session == null -> Text("Starting interactive shell…")
                else -> AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { viewContext ->
                        TerminalView(viewContext, null).apply {
                            setBackgroundColor(Color.rgb(16, 19, 24))
                            setTextSize(16)
                            setTerminalViewClient(PtyTerminalViewClient)
                            attachSession(session)
                        }
                    }
                )
            }
        }
    }
}

private fun PtyTerminalService.LocalBinder.sessionOrNull(): TerminalSession? =
    runCatching { session }.getOrNull()

private object PtyTerminalViewClient : TerminalViewClient {
    override fun onScale(scale: Float): Float = scale
    override fun onSingleTapUp(e: MotionEvent) = Unit
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
