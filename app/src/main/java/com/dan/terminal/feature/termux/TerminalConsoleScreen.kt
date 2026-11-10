package com.dan.terminal.feature.termux

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dan.terminal.core.security.AccessCapability
import com.dan.terminal.di.LocalAppContainer

@Composable
fun TerminalConsoleScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val history by TermuxCommandResults.entries.collectAsState()
    val pending = history.any { it.state == CommandState.RUNNING }
    var command by rememberSaveable { mutableStateOf("uname -a") }
    var message by rememberSaveable { mutableStateOf<String?>(null) }

    fun submit() {
        val cmd = command.trim()
        if (cmd.isEmpty()) return
        if (!container.accessCoordinator.require(AccessCapability.TERMUX_APP)) return
        if (!container.accessCoordinator.require(AccessCapability.TERMUX_RUN_COMMAND_PERMISSION)) return
        message = if (container.termuxBridge.runCommandWithResult(cmd)) null
            else "Unable to start. Check Termux permissions and allow-external-apps."
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("DAN Console", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Bash commands run inside Termux and return output after exit. " +
                "This is not yet an interactive PTY: cd, exports, editors and long-running " +
                "sessions do not persist across commands.",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
            value = command,
            onValueChange = { if (it.length <= 4096) command = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Command") },
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { submit() }, enabled = !pending && command.isNotBlank()) {
                Text(if (pending) "Running…" else "Execute")
            }
            OutlinedButton(onClick = {
                TermuxCommandResults.clear()
                message = "Display cleared. Any running Termux command was not cancelled."
            }) { Text("Clear") }
            OutlinedButton(onClick = onBack) { Text("Back") }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(history, key = { it.id }) { entry ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$ " + entry.command, color = MaterialTheme.colorScheme.primary)
                    when (entry.state) {
                        CommandState.RUNNING -> Text("Waiting for Termux result…")
                        else -> {
                            SelectionContainer {
                                Column {
                                    if (entry.stdout.isNotEmpty()) Text(entry.stdout)
                                    if (entry.stderr.isNotEmpty()) Text(
                                        entry.stderr,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    if (entry.error != null) Text(
                                        entry.error,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text("Exit code: " + (entry.exitCode ?: "?"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
