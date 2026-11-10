package com.dan.terminal.core.security

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class PermissionBroker(private val context: Context) {

    fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun isBluetoothScanGranted(): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            isGranted(Manifest.permission.BLUETOOTH_SCAN)
        else ->
            isGranted(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun isBluetoothConnectGranted(): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            isGranted(Manifest.permission.BLUETOOTH_CONNECT)
        else ->
            isGranted(Manifest.permission.BLUETOOTH)
    }

    fun runtimePermissions(capability: AccessCapability): Array<String> =
        RuntimePermissionPolicy.forCapability(capability, Build.VERSION.SDK_INT)
}
