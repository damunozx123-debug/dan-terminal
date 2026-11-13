package com.dan.terminal.feature.termux

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/**
 * Persistent command shell prototype, NOT yet a PTY/ANSI terminal emulator.
 * Session survives screen recomposition and navigation while its ViewModel is retained.
 */
@Composable
fun LocalShellScreen(
    onSettings: () -> Unit,
    onLegacyConsole: () -> Unit,
    shellModel: LocalShellViewModel? = null
) {
    val activity = LocalContext.current as ComponentActivity
    val retainedModel: LocalShellViewModel = shellModel ?: viewModel(viewModelStoreOwner = activity)
    val session = retainedModel.session
    val output by session.output.collectAsState()
    val running by session.running.collectAsState()
    var command by remember { mutableStateOf("") }
    val scroll = rememberScrollState()
    LaunchedEffect(output.length) { scroll.animateScrollTo(scroll.maxValue) }

    Column(Modifier.fillMaxSize().background(Color(0xFF101318)).padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("DAN Terminal", color = Color(0xFFB7E4C7), modifier = Modifier.weight(1f))
            TextButton(onClick = onSettings) { Text("Setup") }
            TextButton(onClick = onLegacyConsole) { Text("Termux") }
        }
        Text(
            "Local shell · ${if (running) "running" else "stopped"} · app sandbox · PTY pending",
            color = Color.LightGray,
            style = MaterialTheme.typography.labelSmall
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            SelectionContainer {
                Text(
                output,
                modifier = Modifier.fillMaxWidth().verticalScroll(scroll),
                color = Color(0xFFE2E8F0),
                fontFamily = FontFamily.Monospace
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                modifier = Modifier.weight(1f),
                label = { Text("Shell input") },
                singleLine = true
            )
            Button(onClick = {
                session.send(command)
                command = ""
            }, enabled = running && command.isNotBlank()) { Text("Enter") }
        }
        TextButton(onClick = { session.start() }, enabled = !running) { Text("Restart shell") }
    }
}
