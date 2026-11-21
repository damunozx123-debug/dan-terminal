package com.dan.terminal.feature.ssh

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp

/** Native in-app SSH; does not launch Termux or load its private key. */
@Composable
fun NativeSshScreen(onBack: () -> Unit) {
    var generation by remember { mutableIntStateOf(0) }
    val controller = remember(generation) { NativeSshController() }
    DisposableEffect(controller) {
        onDispose { controller.disconnect() }
    }

    var host by remember { mutableStateOf("100.72.159.123") }
    var username by remember { mutableStateOf("dan") }
    var port by remember { mutableStateOf("22") }
    var fingerprint by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fontSp by remember { mutableIntStateOf(14) }

    val status by controller.status.collectAsState()
    val connected by controller.connected.collectAsState()

    Column(Modifier.fillMaxSize().imePadding().padding(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TextButton(onClick = onBack) { Text("Back") }
            Text(
                "BlackArch SSH · Native",
                modifier = Modifier.weight(1f).padding(top = 12.dp),
                style = MaterialTheme.typography.titleSmall
            )
            if (connected) {
                TextButton(onClick = { generation += 1 }) { Text("Disconnect") }
            }
        }

        Text(status, style = MaterialTheme.typography.labelSmall)

        if (!connected) {
            OutlinedTextField(
                value = host, onValueChange = { host = it.take(255) },
                label = { Text("Host / Tailscale IP") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = username, onValueChange = { username = it.take(64) },
                    label = { Text("User") }, singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = port, onValueChange = { port = it.filter(Char::isDigit).take(5) },
                    label = { Text("Port") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.5f)
                )
            }
            OutlinedTextField(
                value = fingerprint,
                onValueChange = { fingerprint = it.trim().take(80) },
                label = { Text("Verified SSH host key (SHA256:…)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("BlackArch login password (not saved)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    controller.connect(
                        NativeSshTarget(host.trim(), port.toIntOrNull() ?: 0,
                            username.trim(), fingerprint.trim()),
                        password
                    )
                    // Do not persist login secrets in preferences or source.
                    password = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Connect directly (verify host first)") }
            Text(
                "Get fingerprint on BlackArch: sudo ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub -E sha256",
                style = MaterialTheme.typography.labelSmall
            )
        }

        if (connected) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val keys = listOf(
                    "Ctrl+C" to "\u0003", "Ctrl+D" to "\u0004",
                    "Esc" to "\u001b", "Tab" to "\t",
                    "↑" to "\u001b[A", "↓" to "\u001b[B",
                    "←" to "\u001b[D", "→" to "\u001b[C",
                    "Enter" to "\r"
                )
                keys.forEach { (label, bytes) ->
                    TextButton(onClick = { controller.sendText(bytes) }) { Text(label) }
                }
                TextButton(onClick = { fontSp = (fontSp - 2).coerceAtLeast(10) }) { Text("A−") }
                TextButton(onClick = { fontSp = (fontSp + 2).coerceAtMost(28) }) { Text("A+") }
            }
        }

        AndroidView(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            factory = { NativeSshTerminalView(it, controller) },
            update = { it.setFontSp(fontSp) }
        )
    }
}
