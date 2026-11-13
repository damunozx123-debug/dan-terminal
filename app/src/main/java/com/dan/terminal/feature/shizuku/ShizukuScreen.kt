package com.dan.terminal.feature.shizuku

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dan.terminal.core.security.AccessCapability
import com.dan.terminal.di.LocalAppContainer

@Composable
fun ShizukuScreen() {
    val container = LocalAppContainer.current
    val state by container.shizukuManager.state.collectAsState()
    var feedback by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Shizuku", style = MaterialTheme.typography.headlineMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Binder alive: ${state.binderAlive}")
                Text("Permission granted: ${state.permissionGranted}")
                Text("Version: ${state.version}")
                Text("Pre-v11: ${state.preV11}")
                state.lastError?.let { Text("Last error: $it") }
                Text(if (state.ready) "Shizuku API access ready. Privileged command execution has not been tested." else "Shizuku API access not ready.")
            }
        }
        Button(
            onClick = { container.accessCoordinator.requestShizukuPermission() },
            enabled = !state.ready && !state.permissionRequestPending,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (state.permissionRequestPending) "Waiting for Shizuku" else "Request Shizuku permission") }
        Button(
            onClick = { container.shizukuManager.openShizukuApp() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Open Shizuku app") }
        Button(
            onClick = {
                container.shizukuManager.recheckPermission()
                val updated = container.shizukuManager.state.value
                feedback = if (updated.ready) {
                    "Refresh complete: Shizuku service and permission are ready."
                } else {
                    "Refresh complete: Shizuku is not ready. " +
                        (updated.lastError ?: "Check Shizuku service and permissions.")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Refresh status") }
        Button(
            onClick = {
                val allowed = container.accessCoordinator.require(AccessCapability.SHIZUKU_PERMISSION)
                feedback = if (allowed) "Access already granted. No permission prompt is necessary." else "Access required. Check the permission flow or open Shizuku."
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Check access requirement") }
        feedback?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}
