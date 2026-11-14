package com.dan.terminal

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import com.dan.terminal.core.bluetooth.BleDevice
import com.dan.terminal.core.bluetooth.toBleDevice
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

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
    @Test
    fun unnamedAdvertisementNeverReadsProtectedDeviceName() {
        val remote = mock(BluetoothDevice::class.java)
        val scan = mock(ScanResult::class.java)
        `when`(scan.device).thenReturn(remote)
        `when`(remote.address).thenReturn("AA:BB:CC:DD:EE:FF")

        assertEquals("Unnamed device", scan.toBleDevice().displayName)
        verify(remote, never()).name
    }
}
