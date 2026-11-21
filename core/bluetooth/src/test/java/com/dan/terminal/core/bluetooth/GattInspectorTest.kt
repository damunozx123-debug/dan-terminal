package com.dan.terminal.core.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*

class GattInspectorTest {
    private val context = mock(Context::class.java)
    private val device = mock(BluetoothDevice::class.java)
    private val gatt = mock(BluetoothGatt::class.java)
    private lateinit var callback: BluetoothGattCallback
    private val inspector = GattInspector(context)

    private fun connect() {
        doAnswer { invocation ->
            callback = invocation.getArgument(2)
            gatt
        }.`when`(device).connectGatt(eq(context), eq(false), any(BluetoothGattCallback::class.java), eq(BluetoothDevice.TRANSPORT_LE))
        `when`(gatt.discoverServices()).thenReturn(true)
        inspector.connect(device)
    }

    @Test fun callbackAfterDisconnectCannotReopenSession() {
        connect()
        inspector.disconnect()
        callback.onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
        assertEquals(GattConnectionState.Disconnected, inspector.state.value)
        verify(gatt, never()).discoverServices()
        verify(gatt).close()
    }

    @Test fun remoteDisconnectClosesGatt() {
        connect()
        callback.onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_DISCONNECTED)
        assertEquals(GattConnectionState.Disconnected, inspector.state.value)
        inspector.disconnect()
        verify(gatt, times(1)).close()
    }

    @Test fun failedConnectionPreservesErrorAndClosesGatt() {
        connect()
        callback.onConnectionStateChange(gatt, 133, BluetoothProfile.STATE_DISCONNECTED)
        assertTrue(inspector.state.value is GattConnectionState.Error)
        verify(gatt).close()
    }

    @Test fun rejectedServiceDiscoveryDoesNotRemainConnected() {
        connect()
        `when`(gatt.discoverServices()).thenReturn(false)
        callback.onConnectionStateChange(gatt, BluetoothGatt.GATT_SUCCESS, BluetoothProfile.STATE_CONNECTED)
        assertTrue(inspector.state.value is GattConnectionState.Error)
        verify(gatt).close()
    }

    @Test fun nullConnectionProducesActionableError() {
        inspector.connect(device)
        assertTrue(inspector.state.value is GattConnectionState.Error)
    }

    @Test fun revokedPermissionFailsWithoutThrowing() {
        doThrow(SecurityException("revoked")).`when`(device)
            .connectGatt(eq(context), eq(false), any(BluetoothGattCallback::class.java), eq(BluetoothDevice.TRANSPORT_LE))
        inspector.connect(device)
        assertTrue(inspector.state.value is GattConnectionState.Error)
    }

    @Test fun staleCallbacksCannotReplaceNewConnection() {
        connect()
        val previousCallback = callback
        connect()
        previousCallback.onConnectionStateChange(gatt, 133, BluetoothProfile.STATE_DISCONNECTED)
        assertEquals(GattConnectionState.Connecting, inspector.state.value)
    }
}
