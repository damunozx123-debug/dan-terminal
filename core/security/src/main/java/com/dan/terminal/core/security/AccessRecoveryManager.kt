package com.dan.terminal.core.security

import android.os.Build

class AccessRecoveryManager(
    private val permissionBroker: PermissionBroker,
    private val shizukuAccess: ShizukuAccess,
    private val termuxAccess: TermuxAccess
) {

    fun assess(capability: AccessCapability): AccessRequirement {
        val shizukuCapability = capability == AccessCapability.SHIZUKU_SERVICE ||
            capability == AccessCapability.SHIZUKU_PERMISSION
        if (shizukuCapability) shizukuAccess.refresh()
        val granted = when (capability) {
            AccessCapability.BLUETOOTH_SCAN -> permissionBroker.isBluetoothScanGranted()
            AccessCapability.BLUETOOTH_CONNECT -> permissionBroker.isBluetoothConnectGranted()
            AccessCapability.SHIZUKU_SERVICE -> shizukuAccess.isBinderAlive
            AccessCapability.SHIZUKU_PERMISSION ->
                shizukuAccess.isBinderAlive && shizukuAccess.isPermissionGranted
            AccessCapability.TERMUX_APP -> termuxAccess.isInstalled()
            AccessCapability.TERMUX_RUN_COMMAND_PERMISSION -> termuxAccess.hasRunCommandPermission()
        }
        // Retain the requested capability while showing the missing prerequisite.
        // Starting the service alone must not dismiss a pending permission request.
        val recoveryCapability = if (shizukuCapability && !shizukuAccess.isBinderAlive) {
            AccessCapability.SHIZUKU_SERVICE
        } else capability
        return AccessRequirement(
            capability = capability,
            granted = granted,
            reason = reason(recoveryCapability),
            steps = steps(recoveryCapability)
        )
    }

    private fun reason(capability: AccessCapability): String = when (capability) {
        AccessCapability.BLUETOOTH_SCAN ->
            "DAN Terminal needs Bluetooth scan access to discover nearby BLE devices."
        AccessCapability.BLUETOOTH_CONNECT ->
            "DAN Terminal needs Bluetooth connect access to inspect GATT services."
        AccessCapability.SHIZUKU_SERVICE ->
            "Shizuku is not running. Privileged operations need the Shizuku service."
        AccessCapability.SHIZUKU_PERMISSION ->
            "DAN Terminal has not been authorised by Shizuku."
        AccessCapability.TERMUX_APP ->
            "Termux is not installed. The Linux runtime bootstrap uses Termux on M1."
        AccessCapability.TERMUX_RUN_COMMAND_PERMISSION ->
            "DAN Terminal cannot send commands to Termux because RUN_COMMAND permission is not granted."
    }

    private fun steps(capability: AccessCapability): List<RecoveryStep> = when (capability) {
        AccessCapability.BLUETOOTH_SCAN,
        AccessCapability.BLUETOOTH_CONNECT -> buildList {
            add(
                RecoveryStep(
                    "Grant access",
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                        "Allow Nearby devices for this Bluetooth operation."
                    else "Allow Location access for BLE scanning on Android 11 and below.",
                    RecoveryAction.REQUEST_RUNTIME_PERMISSION
                )
            )
            if (capability == AccessCapability.BLUETOOTH_SCAN && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                add(
                    RecoveryStep(
                        "Enable Location services",
                        "On Android 11 and below, BLE scanning also requires system Location to be on."
                    )
                )
            }
            add(
                RecoveryStep(
                    "Review denied permissions",
                    "If Android no longer shows the permission prompt, open DAN Terminal's Permissions in App Info.",
                    RecoveryAction.OPEN_APP_SETTINGS
                )
            )
            add(
                RecoveryStep(
                    "Verify access",
                    "Return to DAN Terminal and verify that the capability is now granted.",
                    RecoveryAction.VERIFY
                )
            )
        }

        AccessCapability.SHIZUKU_SERVICE -> listOf(
            RecoveryStep(
                "Install Shizuku",
                "Install the official Shizuku app from the official source. DAN Terminal does not bundle a privileged service in M1.",
                RecoveryAction.OPEN_SHIZUKU_DOWNLOAD
            ),
            RecoveryStep(
                "Enable Developer options",
                "Open Settings -> About phone -> tap Build number seven times.",
                RecoveryAction.OPEN_DEVELOPER_OPTIONS
            ),
            RecoveryStep(
                "Enable Wireless debugging",
                "In Developer options, enable Wireless debugging. Android 11+ lets Shizuku start without a computer.",
                RecoveryAction.OPEN_DEVELOPER_OPTIONS
            ),
            RecoveryStep(
                "Pair and start Shizuku",
                "Open Shizuku, use its pairing flow with Wireless debugging, then start the service.",
                RecoveryAction.OPEN_SHIZUKU
            ),
            RecoveryStep(
                "Verify service",
                "Return to DAN Terminal and verify that the Shizuku binder is alive.",
                RecoveryAction.VERIFY
            )
        )

        AccessCapability.SHIZUKU_PERMISSION -> listOf(
            RecoveryStep(
                "Open Shizuku",
                "Open Shizuku and grant DAN Terminal the API permission.",
                RecoveryAction.OPEN_SHIZUKU
            ),
            RecoveryStep(
                "Request authorisation",
                "Ask Shizuku for permission from within DAN Terminal.",
                RecoveryAction.REQUEST_SHIZUKU_PERMISSION
            ),
            RecoveryStep(
                "Verify authorisation",
                "Re-check the Shizuku permission state.",
                RecoveryAction.VERIFY
            )
        )

        AccessCapability.TERMUX_APP -> listOf(
            RecoveryStep(
                "Install Termux",
                "Install the supported Termux build from F-Droid or the official GitHub releases.",
                RecoveryAction.OPEN_TERMUX_DOWNLOAD
            ),
            RecoveryStep(
                "Verify installation",
                "Return to DAN Terminal; the Termux package will be detected automatically.",
                RecoveryAction.VERIFY
            )
        )

        AccessCapability.TERMUX_RUN_COMMAND_PERMISSION -> listOf(
            RecoveryStep(
                "Grant RUN_COMMAND permission",
                "Allow DAN Terminal to run commands in the Termux environment.",
                RecoveryAction.REQUEST_RUNTIME_PERMISSION
            ),
            RecoveryStep(
                "Review denied permission",
                "Open DAN Terminal App Info -> Permissions and allow 'Run commands in Termux environment'.",
                RecoveryAction.OPEN_APP_SETTINGS
            ),
            RecoveryStep(
                "Allow external apps in Termux",
                "In Termux, run: mkdir -p ~/.termux && echo 'allow-external-apps=true' >> ~/.termux/termux.properties && termux-reload-settings"
            ),
            RecoveryStep(
                "Verify permission",
                "Re-check the permission after returning.",
                RecoveryAction.VERIFY
            )
        )
    }
}
