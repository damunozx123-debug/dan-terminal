package com.dan.terminal.core.security

import android.Manifest

/** Only permissions needed for the user's requested operation. */
object RuntimePermissionPolicy {
    fun forCapability(capability: AccessCapability, sdkInt: Int): Array<String> = when (capability) {
        AccessCapability.BLUETOOTH_SCAN -> if (sdkInt >= 31) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        AccessCapability.BLUETOOTH_CONNECT -> if (sdkInt >= 31) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
        } else emptyArray()
        AccessCapability.TERMUX_RUN_COMMAND_PERMISSION -> arrayOf("com.termux.permission.RUN_COMMAND")
        else -> emptyArray()
    }
}
