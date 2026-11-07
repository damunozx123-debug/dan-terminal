package com.dan.terminal

import android.bluetooth.BluetoothDevice
import com.dan.terminal.core.bluetooth.BleDevice
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class BleDeviceTest {

    @Test
    fun unnamedDeviceUsesFallbackDisplayName() {
        val device = BleDevice(
            name = null,
            address = "AA:BB:CC:DD:EE:FF",
            rssi = -42,
            serviceUuids = emptyList(),
            manufacturerData = emptyMap(),
            raw = mock(BluetoothDevice::class.java)
        )
        assertEquals("Unnamed device", device.displayName)
    }

    @Test
    fun blankNameUsesFallbackDisplayName() {
        val device = BleDevice(
            name = "   ",
            address = "AA:BB:CC:DD:EE:FF",
            rssi = -42,
            serviceUuids = emptyList(),
            manufacturerData = emptyMap(),
            raw = mock(BluetoothDevice::class.java)
        )
        assertEquals("Unnamed device", device.displayName)
    }
}
