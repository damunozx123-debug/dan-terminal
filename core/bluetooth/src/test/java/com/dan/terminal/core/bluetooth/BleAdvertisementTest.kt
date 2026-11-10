package com.dan.terminal.core.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*

class BleAdvertisementTest {
    private val device = mock(BluetoothDevice::class.java)
    private val result = mock(ScanResult::class.java).also {
        `when`(it.device).thenReturn(device)
        `when`(device.address).thenReturn("00:11:22:33:44:55")
    }

    @Test fun revokedConnectPermissionStillAllowsUnnamedAdvertisement() {
        `when`(device.name).thenThrow(SecurityException("Permission revoked"))
        assertEquals("Unnamed device", result.toBleDevice().displayName)
    }

    @Test fun advertisedNameDoesNotReadProtectedDeviceName() {
        val record = mock(ScanRecord::class.java)
        `when`(result.scanRecord).thenReturn(record)
        `when`(record.deviceName).thenReturn("My light")
        assertEquals("My light", result.toBleDevice().displayName)
        verify(device, never()).name
    }

    @Test fun nonConnectableAdvertisementDoesNotOfferConnection() {
        `when`(result.isConnectable).thenReturn(false)
        assertFalse(result.toBleDevice().isConnectable)
    }
}
