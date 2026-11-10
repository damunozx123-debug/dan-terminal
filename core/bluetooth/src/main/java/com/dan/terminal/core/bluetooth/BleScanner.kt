package com.dan.terminal.core.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class BleScanner(private val context: Context) {

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private val adapter get() = bluetoothManager?.adapter

    val isBluetoothEnabled: Boolean get() = try {
        adapter?.isEnabled == true
    } catch (_: SecurityException) { false }

    val isLeSupported: Boolean get() =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    @SuppressLint("MissingPermission")
    fun scan(): Flow<BleDevice> = callbackFlow {
        val scanner = adapter?.bluetoothLeScanner
            ?: throw IllegalStateException("BLE scanner is unavailable")

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                try {
                    trySend(result.toBleDevice())
                } catch (error: SecurityException) {
                    close(error)
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { onScanResult(0, it) }
            }

            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException("BLE scan failed: $errorCode"))
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, callback)
        awaitClose { runCatching { scanner.stopScan(callback) } }
    }
}
