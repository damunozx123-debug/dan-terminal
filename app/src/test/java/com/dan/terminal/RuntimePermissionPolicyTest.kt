package com.dan.terminal

import com.dan.terminal.core.security.AccessCapability
import com.dan.terminal.core.security.RuntimePermissionPolicy
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class RuntimePermissionPolicyTest {
    @Test fun termuxRequestNeverAsksForBluetooth() {
        assertArrayEquals(arrayOf("com.termux.permission.RUN_COMMAND"),
            RuntimePermissionPolicy.forCapability(AccessCapability.TERMUX_RUN_COMMAND_PERMISSION, 36))
    }
    @Test fun scanRequestsOnlyScanOnModernAndroid() {
        assertArrayEquals(arrayOf("android.permission.BLUETOOTH_SCAN"),
            RuntimePermissionPolicy.forCapability(AccessCapability.BLUETOOTH_SCAN, 36))
    }
    @Test fun connectionRequestsOnlyConnectOnModernAndroid() {
        assertArrayEquals(arrayOf("android.permission.BLUETOOTH_CONNECT"),
            RuntimePermissionPolicy.forCapability(AccessCapability.BLUETOOTH_CONNECT, 31))
    }
    @Test fun legacyScanNeedsLocationButConnectionNeedsNoRuntimeGrant() {
        assertArrayEquals(arrayOf("android.permission.ACCESS_FINE_LOCATION"),
            RuntimePermissionPolicy.forCapability(AccessCapability.BLUETOOTH_SCAN, 30))
        assertArrayEquals(emptyArray<String>(),
            RuntimePermissionPolicy.forCapability(AccessCapability.BLUETOOTH_CONNECT, 30))
    }
    @Test fun shizukuDoesNotRequestAndroidRuntimePermissions() {
        assertArrayEquals(emptyArray<String>(),
            RuntimePermissionPolicy.forCapability(AccessCapability.SHIZUKU_PERMISSION, 36))
    }
}
