package com.dan.terminal

import android.content.Context
import com.dan.terminal.core.bluetooth.BleScanner
import com.dan.terminal.core.security.AccessCoordinator
import com.dan.terminal.core.security.AccessRecoveryManager
import com.dan.terminal.core.security.PermissionBroker
import com.dan.terminal.core.security.SecureStorage
import com.dan.terminal.core.shizuku.ShizukuManager
import com.dan.terminal.feature.termux.TermuxBridge

class AppContainer(context: Context) {

    val permissionBroker = PermissionBroker(context)
    val secureStorage = SecureStorage(context)
    val shizukuManager = ShizukuManager(context)
    val termuxBridge = TermuxBridge(context)
    val bleScanner = BleScanner(context)

    val recoveryManager = AccessRecoveryManager(
        permissionBroker = permissionBroker,
        shizukuAccess = shizukuManager,
        termuxAccess = termuxBridge
    )

    val accessCoordinator = AccessCoordinator(
        recoveryManager = recoveryManager,
        shizukuAccess = shizukuManager
    )
}
