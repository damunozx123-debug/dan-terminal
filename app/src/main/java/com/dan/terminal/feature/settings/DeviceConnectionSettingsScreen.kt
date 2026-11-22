package com.dan.terminal.feature.settings

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

internal fun batteryPercent(level: Int, scale: Int): Int? =
    if (level < 0 || scale <= 0 || level > scale) null else (level * 100 / scale)

internal fun batteryDisplay(percent: Int?): String =
    percent?.let { "$it%" } ?: "Not reported"

internal data class ConnectionStatus(
    val phoneBattery: String,
    val charging: String,
    val wifi: String,
    val network: String,
    val bluetooth: String,
    val hotspot: String
)

/** Reads only OS-provided device state. Does not scan or change radios. */
internal fun readConnectionStatus(context: Context): ConnectionStatus {
    val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val percent = batteryPercent(
        battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1,
        battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    )
    val charging = when (battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_FULL -> "Fully charged"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "On battery"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
        else -> "Unknown"
    }

    val wifi = runCatching {
        val manager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        if (manager?.isWifiEnabled == true) "Enabled" else "Disabled or unavailable"
    }.getOrDefault("Unavailable")

    val network = runCatching {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = manager.getNetworkCapabilities(manager.activeNetwork)
        when {
            caps == null -> "No active default network"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN / private tunnel"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth tethering"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet / USB network"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
            else -> "Other network"
        }
    }.getOrDefault("Network status unavailable")

    val bluetooth = runCatching {
        if (Build.VERSION.SDK_INT >= 31 &&
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) "Permission needed"
        else {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            when (manager?.adapter?.isEnabled) {
                true -> "Enabled"
                false -> "Disabled"
                null -> "Not supported"
            }
        }
    }.getOrDefault("Unavailable")

    return ConnectionStatus(
        phoneBattery = batteryDisplay(percent),
        charging = charging,
        wifi = wifi,
        network = network,
        bluetooth = bluetooth,
        hotspot = "Open Android settings to check or change"
    )
}

/** User tap only. Do not toggle radios or attempt privileged tethering calls. */
internal fun openSystemSettings(context: Context, action: String): Boolean {
    val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (runCatching { context.startActivity(intent) }.isSuccess) return true
    return runCatching {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
}

@Composable
fun DeviceConnectionSettingsScreen(
    onBack: () -> Unit,
    onNativeSsh: () -> Unit,
    onBluetooth: () -> Unit,
    onDiagnostics: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var status by remember(context) { mutableStateOf(readConnectionStatus(context)) }
    var message by remember { mutableStateOf<String?>(null) }

    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) status = readConnectionStatus(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    fun launch(action: String) {
        message = if (openSystemSettings(context, action)) null
                  else "Android settings could not be opened on this device."
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Device & connections", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Back") }
        }
        Text("Your phone status and remote access, without hidden background scans.",
            style = MaterialTheme.typography.bodyMedium)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Phone battery", style = MaterialTheme.typography.titleMedium)
                Text(status.phoneBattery, style = MaterialTheme.typography.headlineMedium)
                Text(status.charging)
            }
        }
        StatusLine("Active connection", status.network)
        StatusLine("Wi-Fi", status.wifi)
        StatusLine("Bluetooth", status.bluetooth)
        StatusLine("Hotspot & tethering", status.hotspot)
        OutlinedButton(onClick = { status = readConnectionStatus(context) }, modifier = Modifier.fillMaxWidth()) {
            Text("Refresh status")
        }
        Text("Connect remotely", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onNativeSsh, modifier = Modifier.fillMaxWidth()) {
            Text("Open secure BlackArch SSH")
        }
        OutlinedButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://127.0.0.1:8765"))
                message = if (runCatching { context.startActivity(intent) }.isSuccess) {
                    "Control Deck opened. If offline, first start the SSH tunnel in Termux."
                } else {
                    "No browser available. Start an SSH tunnel, then open localhost:8765."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Launch DAN Control Deck") }
        Text(
            "For the mini-desktop, keep this Termux tunnel open: ssh -N -L 8765:127.0.0.1:8765 dan@100.72.159.123",
            style = MaterialTheme.typography.bodySmall
        )
        Text("Requires your independently verified SSH host fingerprint. Remote laptop battery is not available until an authenticated status feed is connected.",
            style = MaterialTheme.typography.bodySmall)
        Text("Phone connection controls", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { launch(Settings.ACTION_WIFI_SETTINGS) }, modifier = Modifier.weight(1f)) {
                Text("Wi-Fi")
            }
            OutlinedButton(onClick = { launch(Settings.ACTION_BLUETOOTH_SETTINGS) }, modifier = Modifier.weight(1f)) {
                Text("Bluetooth")
            }
        }
        OutlinedButton(
            onClick = { launch("android.settings.TETHER_SETTINGS") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Hotspot / USB / Bluetooth tethering") }
        OutlinedButton(onClick = onBluetooth, modifier = Modifier.fillMaxWidth()) {
            Text("Discover and inspect Bluetooth devices")
        }
        OutlinedButton(onClick = onDiagnostics, modifier = Modifier.fillMaxWidth()) {
            Text("Run connectivity diagnostics")
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Controls open system settings: DAN Terminal does not silently toggle radios, reveal hotspot passwords or claim unauthorized device battery readings.",
            style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun StatusLine(title: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
