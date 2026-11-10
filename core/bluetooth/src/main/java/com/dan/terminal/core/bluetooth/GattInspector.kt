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

/** Owns one connection. Each attempt has its own callback identity. */
class GattInspector(private val context: Context) {
    private class Session(var connection: BluetoothGatt? = null)
    private var session: Session? = null
    private val _state = MutableStateFlow<GattConnectionState>(GattConnectionState.Idle)
    val state: StateFlow<GattConnectionState> = _state.asStateFlow()
    private val _lastRead = MutableStateFlow<Pair<String, ByteArray>?>(null)
    val lastRead: StateFlow<Pair<String, ByteArray>?> = _lastRead.asStateFlow()

    @Synchronized
    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        disconnect()
        val attempt = Session()
        session = attempt
        _state.value = GattConnectionState.Connecting
        try {
            val connection = device.connectGatt(context, false, callback(attempt), BluetoothDevice.TRANSPORT_LE)
            if (session !== attempt) {
                // A synchronous terminal callback may already have closed this connection.
                if (connection !== attempt.connection) runCatching { connection?.close() }
            } else if (connection == null) {
                finish(GattConnectionState.Error("Could not start GATT. Check Bluetooth and try again."))
            } else {
                attempt.connection = connection
            }
        } catch (_: SecurityException) {
            finish(GattConnectionState.Error("Bluetooth connection permission is missing. Grant access and reconnect."))
        } catch (_: Exception) {
            finish(GattConnectionState.Error("Could not start GATT. Check Bluetooth and try again."))
        }
    }

    @Synchronized
    fun disconnect() = finish(GattConnectionState.Disconnected)

    @SuppressLint("MissingPermission")
    private fun finish(state: GattConnectionState) {
        val previous = session
        session = null // Invalidate callbacks before asking Android to disconnect.
        _lastRead.value = null
        _state.value = state
        runCatching { previous?.connection?.disconnect() }
        runCatching { previous?.connection?.close() }
    }

    @Synchronized
    @SuppressLint("MissingPermission")
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic): Boolean {
        val connection = session?.connection ?: return false
        return try {
            connection.readCharacteristic(characteristic)
        } catch (_: SecurityException) {
            finish(GattConnectionState.Error("Bluetooth permission was revoked. Grant access and reconnect."))
            false
        }
    }

    /** Called under this inspector's monitor, including for early callbacks. */
    private fun accepts(attempt: Session, connection: BluetoothGatt): Boolean {
        if (session !== attempt) return false
        if (attempt.connection == null) attempt.connection = connection
        return attempt.connection === connection
    }

    private fun callback(attempt: Session) = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            synchronized(this@GattInspector) {
                if (!accepts(attempt, gatt)) return
                when {
                    status != BluetoothGatt.GATT_SUCCESS ->
                        finish(GattConnectionState.Error("GATT connection failed: $status. Try reconnecting."))
                    newState == BluetoothProfile.STATE_DISCONNECTED ->
                        finish(GattConnectionState.Disconnected)
                    newState == BluetoothProfile.STATE_CONNECTED -> {
                        _state.value = GattConnectionState.Connected
                        try {
                            if (!gatt.discoverServices()) {
                                finish(GattConnectionState.Error("Service discovery could not start. Try reconnecting."))
                            }
                        } catch (_: SecurityException) {
                            finish(GattConnectionState.Error("Bluetooth permission was revoked. Grant access and reconnect."))
                        }
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            synchronized(this@GattInspector) {
                if (!accepts(attempt, gatt)) return
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    _state.value = GattConnectionState.Services(gatt.services.toList())
                } else {
                    finish(GattConnectionState.Error("Service discovery failed: $status"))
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            synchronized(this@GattInspector) {
                if (accepts(attempt, gatt) && status == BluetoothGatt.GATT_SUCCESS) {
                    _lastRead.value = characteristic.uuid.toString() to value.copyOf()
                }
            }
        }

        @Suppress("DEPRECATION")
        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                val value = characteristic.value ?: return
                onCharacteristicRead(gatt, characteristic, value, status)
            }
        }
    }
}
