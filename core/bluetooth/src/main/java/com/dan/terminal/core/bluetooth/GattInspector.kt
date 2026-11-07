package com.dan.terminal.core.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface GattConnectionState {
    data object Idle : GattConnectionState
    data object Connecting : GattConnectionState
    data object Connected : GattConnectionState
    data class Services(val services: List<BluetoothGattService>) : GattConnectionState
    data class Error(val message: String) : GattConnectionState
    data object Disconnected : GattConnectionState
}

class GattInspector(private val context: Context) {

    private var gatt: BluetoothGatt? = null

    private val _state = MutableStateFlow<GattConnectionState>(GattConnectionState.Idle)
    val state: StateFlow<GattConnectionState> = _state.asStateFlow()

    private val _lastRead = MutableStateFlow<Pair<String, ByteArray>?>(null)
    val lastRead: StateFlow<Pair<String, ByteArray>?> = _lastRead.asStateFlow()

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        disconnect()
        _state.value = GattConnectionState.Connecting
        gatt = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        runCatching { gatt?.disconnect() }
        runCatching { gatt?.close() }
        gatt = null
        _state.value = GattConnectionState.Disconnected
    }

    @SuppressLint("MissingPermission")
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        gatt?.readCharacteristic(characteristic)
    }

    private val callback = object : BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS &&
                newState == BluetoothProfile.STATE_CONNECTED
            ) {
                _state.value = GattConnectionState.Connected
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _state.value = GattConnectionState.Disconnected
            } else {
                _state.value = GattConnectionState.Error("GATT state=$newState status=$status")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _state.value = GattConnectionState.Services(gatt.services)
            } else {
                _state.value = GattConnectionState.Error("Service discovery failed: $status")
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _lastRead.value = characteristic.uuid.toString() to value
            }
        }

        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
                status == BluetoothGatt.GATT_SUCCESS
            ) {
                val value = characteristic.value ?: return
                _lastRead.value = characteristic.uuid.toString() to value
            }
        }
    }
}
