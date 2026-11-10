package com.dan.terminal.feature.bluetooth

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dan.terminal.core.bluetooth.BleDevice
import com.dan.terminal.core.bluetooth.GattConnectionState
import com.dan.terminal.core.bluetooth.GattInspector
import com.dan.terminal.core.security.AccessCapability
import com.dan.terminal.di.LocalAppContainer

@Composable
fun GattScreen(
    device: BleDevice?,
    onBack: () -> Unit
) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val inspector = remember(context) { GattInspector(context) }
    val state by inspector.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, inspector) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) inspector.disconnect()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            inspector.disconnect()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("GATT inspector", style = MaterialTheme.typography.headlineMedium)
        Text(device?.let { "${it.displayName} (${it.address})" } ?: "No device selected")

        Button(
            onClick = {
                if (device == null) return@Button
                if (container.accessCoordinator.require(AccessCapability.BLUETOOTH_CONNECT) &&
                    container.permissionBroker.isBluetoothConnectGranted()
                ) {
                    inspector.connect(device.raw)
                }
            },
            enabled = device?.isConnectable == true && state != GattConnectionState.Connecting,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Connect") }

        Button(
            onClick = { inspector.disconnect() },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Disconnect") }

        when (val current = state) {
            is GattConnectionState.Services -> {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(current.services) { service ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(service.uuid.toString())
                                service.characteristics.forEach { c ->
                                    Text("  ${c.uuid}  props=${c.properties}")
                                }
                            }
                        }
                    }
                }
            }
            else -> Text(current.toString())
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
    }
}
