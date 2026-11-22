package com.dan.terminal.feature.termux

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dan.terminal.core.security.AccessCapability
import com.dan.terminal.di.LocalAppContainer

/**
 * Operator-observed tool catalog, NOT a live installed-tool detector.
 * Every execution requires a user tap plus the existing Termux permission broker.
 * Only fixed help/version commands may be launched. No scanning is automatic.
 */
private data class AvailableTool(
    val label: String,
    val environment: String,
    val helpCommand: String?
)

private val observedTools = listOf(
    AvailableTool("Nmap", "Termux native", "nmap --help"),
    AvailableTool("ADB", "Termux native", "adb help"),
    AvailableTool("Fastboot", "Termux native", "fastboot --help"),
    AvailableTool("Python", "Termux native", "python --version"),
    AvailableTool("Git", "Termux native", "git --version"),
    AvailableTool("SSH", "Termux native", "ssh -V"),
    AvailableTool("Ollama client", "Termux native (server not verified)", "ollama --help"),
    AvailableTool("Nmap", "Debian proot", "nmap --help"),
    AvailableTool("SQLmap", "Debian proot", "sqlmap --help"),
    AvailableTool("Gobuster", "Debian proot", "gobuster --help"),
    AvailableTool("ffuf", "Debian proot", "ffuf -h"),
    AvailableTool("Hydra", "Debian proot", "hydra -h"),
    AvailableTool("dig", "Debian proot", "dig -h"),
    AvailableTool("netcat", "Debian proot", "nc -h"),
    AvailableTool("Nikto", "Debian proot (unverified installation)", null),
    AvailableTool("BlackArch extended toolkit", "Remote: SSH only", null),
    AvailableTool("Qwen2.5-Coder", "BlackArch via private Termux SSH tunnel", null)
)

@Composable
fun SecurityToolsScreen() {
    val container = LocalAppContainer.current
    var message by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Security tool launcher", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Inventory observed 22 Nov 2026. Availability is not rechecked live. " +
                "Each button opens a help command only; no target scanning or root access.",
            style = MaterialTheme.typography.bodySmall
        )
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(observedTools) { tool ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(tool.label, style = MaterialTheme.typography.titleMedium)
                        Text(tool.environment, style = MaterialTheme.typography.bodySmall)
                        if (tool.helpCommand != null) {
                            Button(onClick = {
                                if (!container.accessCoordinator.require(AccessCapability.TERMUX_APP) ||
                                    !container.accessCoordinator.require(AccessCapability.TERMUX_RUN_COMMAND_PERMISSION)
                                ) return@Button
                                // All command strings are developer-defined; never interpolate target input.
                                val command = if (tool.environment.startsWith("Debian")) {
                                    "proot-distro login debian -- bash -lc '" + tool.helpCommand + "'"
                                } else {
                                    tool.helpCommand
                                }
                                val submitted = container.termuxBridge.runCommand(
                                    command, background = false
                                )
                                message = if (submitted) {
                                    "Help command submitted to Termux. Execution must be checked there."
                                } else {
                                    "Termux did not accept the request. Check RUN_COMMAND permission."
                                }
                            }) { Text("Open help in Termux") }
                        } else {
                            Text("Not launchable until verified and integrated.")
                        }
                    }
                }
            }
        }
    }
}
