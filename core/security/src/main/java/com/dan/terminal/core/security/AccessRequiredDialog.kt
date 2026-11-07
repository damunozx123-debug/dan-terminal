package com.dan.terminal.core.security

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun AccessRequiredDialog(
    requirement: AccessRequirement,
    coordinator: AccessCoordinator,
    permissionBroker: PermissionBroker,
    onOpenShizukuApp: () -> Unit,
    onOpenShizukuDownload: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        coordinator.verifyPending()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Access Required") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(requirement.reason, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                requirement.steps.forEachIndexed { index, step ->
                    Text(
                        "${index + 1}. ${step.title}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(step.description, style = MaterialTheme.typography.bodySmall)
                    if (step.action != null) {
                        TextButton(
                            onClick = {
                                when (step.action) {
                                    RecoveryAction.REQUEST_RUNTIME_PERMISSION ->
                                        permissionLauncher.launch(permissionBroker.bluetoothPermissions())

                                    RecoveryAction.REQUEST_SHIZUKU_PERMISSION ->
                                        coordinator.requestShizukuPermission()

                                    RecoveryAction.OPEN_DEVELOPER_OPTIONS ->
                                        RecoveryIntents.openDeveloperOptions(context)

                                    RecoveryAction.OPEN_SHIZUKU ->
                                        onOpenShizukuApp()

                                    RecoveryAction.OPEN_SHIZUKU_DOWNLOAD ->
                                        onOpenShizukuDownload()

                                    RecoveryAction.OPEN_APP_SETTINGS ->
                                        RecoveryIntents.openAppSettings(
                                            context,
                                            "com.dan.terminal"
                                        )

                                    RecoveryAction.OPEN_TERMUX_DOWNLOAD ->
                                        RecoveryIntents.openUrl(
                                            context,
                                            "https://f-droid.org/packages/com.termux/"
                                        )

                                    RecoveryAction.VERIFY ->
                                        coordinator.verifyPending()

                                    null -> Unit
                                }
                            }
                        ) {
                            Text(actionLabel(step.action))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { coordinator.verifyPending() }) {
                Text("Verify")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}

private fun actionLabel(action: RecoveryAction): String = when (action) {
    RecoveryAction.REQUEST_RUNTIME_PERMISSION -> "Grant permission"
    RecoveryAction.REQUEST_SHIZUKU_PERMISSION -> "Request Shizuku permission"
    RecoveryAction.OPEN_DEVELOPER_OPTIONS -> "Open Developer options"
    RecoveryAction.OPEN_SHIZUKU -> "Open Shizuku"
    RecoveryAction.OPEN_SHIZUKU_DOWNLOAD -> "Download Shizuku"
    RecoveryAction.OPEN_APP_SETTINGS -> "Open App Info"
    RecoveryAction.OPEN_TERMUX_DOWNLOAD -> "Get Termux"
    RecoveryAction.VERIFY -> "Verify"
}
