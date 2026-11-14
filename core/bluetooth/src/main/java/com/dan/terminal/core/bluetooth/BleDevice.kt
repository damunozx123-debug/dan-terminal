package com.dan.terminal.core.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import java.util.UUID

data class BleDevice(
    val name: String?,
    val address: String,
    val rssi: Int,
    val serviceUuids: List<UUID>,
    val manufacturerData: Map<Int, ByteArray>,
    val raw: BluetoothDevice,
    val isConnectable: Boolean = true
) {
    val displayName: String get() = name?.takeIf { it.isNotBlank() } ?: "Unnamed device"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BleDevice) return false
        return address == other.address && rssi == other.rssi && name == other.name &&
            isConnectable == other.isConnectable && serviceUuids == other.serviceUuids &&
            manufacturerData.keys == other.manufacturerData.keys &&
            manufacturerData.all { (key, value) -> value.contentEquals(other.manufacturerData[key]) }
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + address.hashCode()
        result = 31 * result + rssi
        result = 31 * result + isConnectable.hashCode()
        result = 31 * result + serviceUuids.hashCode()
        result = 31 * result + manufacturerData.entries.sumOf { it.key.hashCode() xor it.value.contentHashCode() }
        return result
    }
}

fun ScanResult.toBleDevice(): BleDevice = BleDevice(
    // Scan advertising data is readable under BLUETOOTH_SCAN. Remote device.name
    // requires BLUETOOTH_CONNECT on Android 12+, which may not be granted.
    name = scanRecord?.deviceName,
    address = device.address,
    rssi = rssi,
    serviceUuids = scanRecord?.serviceUuids?.map { it.uuid }.orEmpty(),
    manufacturerData = scanRecord?.manufacturerSpecificData?.let { data ->
        buildMap {
            for (i in 0 until data.size()) {
                put(data.keyAt(i), data.valueAt(i).copyOf())
            }
        }
    }.orEmpty(),
    raw = device,
    isConnectable = isConnectable
)
