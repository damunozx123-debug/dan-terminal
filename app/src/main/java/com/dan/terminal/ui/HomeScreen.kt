package com.dan.terminal.ui

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
import com.dan.terminal.di.LocalAppContainer

@Composable
fun HomeScreen(
    onBluetooth: () -> Unit,
    onShizuku: () -> Unit,
    onTermux: () -> Unit
) {
    val container = LocalAppContainer.current
    val shizuku by container.shizukuManager.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("DAN Terminal", style = MaterialTheme.typography.headlineMedium)
        Text("M0 + M1 baseline", style = MaterialTheme.typography.bodyMedium)
        StatusCard(
            title = "Shizuku",
            value = when {
                shizuku.ready -> "Ready (v${shizuku.version})"
                shizuku.binderAlive -> "Binder alive, permission missing"
                else -> "Not running"
            }
        )
        StatusCard("Termux", if (container.termuxBridge.isInstalled()) "Installed" else "Not installed")
        StatusCard("Bluetooth", if (container.bleScanner.isBluetoothEnabled) "Enabled" else "Disabled")

        Button(onClick = onBluetooth, modifier = Modifier.fillMaxWidth()) {
            Text("Bluetooth / BLE / GATT")
        }
        Button(onClick = onShizuku, modifier = Modifier.fillMaxWidth()) {
            Text("Shizuku privilege broker")
        }
        Button(onClick = onTermux, modifier = Modifier.fillMaxWidth()) {
            Text("Termux runtime bootstrap")
        }
    }
}

@Composable
private fun StatusCard(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
