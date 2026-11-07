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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dan.terminal.core.security.AccessCapability
import com.dan.terminal.di.LocalAppContainer

@Composable
fun ShizukuScreen() {
    val container = LocalAppContainer.current
    val state by container.shizukuManager.state.collectAsState()

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
            }
        }
        Button(
            onClick = { container.shizukuManager.requestPermission() },
            enabled = state.binderAlive && !state.permissionGranted,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Request Shizuku permission") }
        Button(
            onClick = { container.shizukuManager.openShizukuApp() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Open Shizuku app") }
        Button(
            onClick = { container.shizukuManager.refresh() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Refresh status") }
        Button(
            onClick = {
                container.accessCoordinator.require(AccessCapability.SHIZUKU_SERVICE)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Show Access Required flow") }
    }
}
