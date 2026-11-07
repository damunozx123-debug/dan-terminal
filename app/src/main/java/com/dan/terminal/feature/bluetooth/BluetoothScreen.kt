package com.dan.terminal.feature.bluetooth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dan.terminal.core.bluetooth.BleDevice
import com.dan.terminal.core.security.AccessCapability
import com.dan.terminal.di.LocalAppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun BluetoothScreen(onDeviceSelected: (BleDevice) -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    var devices by remember { mutableStateOf<List<BleDevice>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun startScan() {
        if (!container.accessCoordinator.require(AccessCapability.BLUETOOTH_SCAN)) return
        if (!container.permissionBroker.isBluetoothScanGranted()) {
            permissionLauncher.launch(container.permissionBroker.bluetoothPermissions())
            return
        }
        scanning = true
        error = null
        devices = emptyList()
        scanJob = scope.launch {
            runCatching {
                container.bleScanner.scan().collect { device ->
                    devices = (devices.filterNot { it.address == device.address } + device)
                        .sortedByDescending { it.rssi }
                }
            }.onFailure { error = it.message }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Bluetooth / BLE", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Native Android BLE scan. Tap a device to inspect GATT services.",
            style = MaterialTheme.typography.bodySmall
        )
        Button(
            onClick = {
                if (scanning) {
                    scanJob?.cancel()
                    scanning = false
                } else {
                    startScan()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (scanning) "Stop scan" else "Start scan") }
        error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(devices, key = { it.address }) { device ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onDeviceSelected(device) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(device.displayName, style = MaterialTheme.typography.titleMedium)
                        Text(device.address)
                        Text("RSSI: ${device.rssi} dBm")
                        if (device.serviceUuids.isNotEmpty()) {
                            Text("Services: ${device.serviceUuids.size}")
                        }
                        if (device.manufacturerData.isNotEmpty()) {
                            Text("Manufacturer data: ${device.manufacturerData.size} entries")
                        }
                    }
                }
            }
        }
    }
}
