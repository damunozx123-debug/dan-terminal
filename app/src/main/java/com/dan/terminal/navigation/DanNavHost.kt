package com.dan.terminal.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dan.terminal.core.bluetooth.BleDevice
import com.dan.terminal.core.security.AccessRequiredDialog
import com.dan.terminal.core.security.AccessCapability
import com.dan.terminal.di.LocalAppContainer
import com.dan.terminal.feature.bluetooth.BluetoothScreen
import com.dan.terminal.feature.bluetooth.GattScreen
import com.dan.terminal.feature.shizuku.ShizukuScreen
import com.dan.terminal.feature.termux.TermuxScreen
import com.dan.terminal.feature.termux.TerminalConsoleScreen
import com.dan.terminal.ui.HomeScreen

@Composable
fun DanNavHost() {
    val navController = rememberNavController()
    val container = LocalAppContainer.current
    val activeRequirement by container.accessCoordinator
        .activeRequirement
        .collectAsState()
    var selectedDevice by remember { mutableStateOf<BleDevice?>(null) }
    val shizuku by container.shizukuManager.state.collectAsState()
    LaunchedEffect(shizuku.binderAlive, shizuku.permissionGranted, shizuku.preV11) {
        container.accessCoordinator.verifyPending()
    }

    activeRequirement?.let { requirement ->
        val needsShizuku = requirement.capability == AccessCapability.SHIZUKU_SERVICE ||
            requirement.capability == AccessCapability.SHIZUKU_PERMISSION
        AccessRequiredDialog(
            requirement = requirement,
            coordinator = container.accessCoordinator,
            permissionBroker = container.permissionBroker,
            onOpenShizukuApp = { container.shizukuManager.openShizukuApp() },
            onOpenShizukuDownload = { container.shizukuManager.openShizukuDownload() },
            shizukuMessage = if (needsShizuku) shizuku.lastError else null,
            shizukuRequestPending = needsShizuku && shizuku.permissionRequestPending,
            onVerify = {
                if (needsShizuku) container.shizukuManager.recheckPermission()
                container.accessCoordinator.verifyPending()
            },
            onDismiss = { container.accessCoordinator.dismiss() }
        )
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onBluetooth = { navController.navigate("bluetooth") },
                onShizuku = { navController.navigate("shizuku") },
                onTermux = { navController.navigate("termux") },
                onConsole = { navController.navigate("console") }
            )
        }
        composable("bluetooth") {
            BluetoothScreen(
                onDeviceSelected = { device ->
                    selectedDevice = device
                    navController.navigate("gatt")
                }
            )
        }
        composable("gatt") {
            GattScreen(
                device = selectedDevice,
                onBack = { navController.popBackStack() }
            )
        }
        composable("shizuku") { ShizukuScreen() }
        composable("termux") { TermuxScreen() }
        composable("console") {
            TerminalConsoleScreen(onBack = { navController.popBackStack() })
        }
    }
}
