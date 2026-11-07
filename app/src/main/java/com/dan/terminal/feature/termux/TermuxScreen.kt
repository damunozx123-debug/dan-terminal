package com.dan.terminal.feature.termux

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

@Composable
fun TermuxScreen() {
    val container = LocalAppContainer.current
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Termux runtime", style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Installed: ${container.termuxBridge.isInstalled()}")
                Text("RUN_COMMAND permission: ${container.termuxBridge.hasRunCommandPermission()}")
            }
        }
        Button(
            onClick = {
                if (!container.accessCoordinator.require(AccessCapability.TERMUX_APP)) return@Button
                if (!container.accessCoordinator.require(
                        AccessCapability.TERMUX_RUN_COMMAND_PERMISSION
                    )
                ) return@Button
                val script = container.termuxBridge.loadBootstrapScript()
                val ok = container.termuxBridge.runBootstrap(script)
                message = if (ok) {
                    "Bootstrap sent to Termux. Check the Termux notification if it does not start automatically."
                } else {
                    "Failed to send bootstrap. Verify Termux is installed, RUN_COMMAND permission is " +
                        "granted, and allow-external-apps=true is set in ~/.termux/termux.properties."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Run idempotent bootstrap") }

        Button(
            onClick = {
                container.accessCoordinator.require(AccessCapability.TERMUX_RUN_COMMAND_PERMISSION)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Check RUN_COMMAND permission") }
        message?.let { Text(it) }
    }
}
