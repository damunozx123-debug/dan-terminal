package com.dan.terminal.core.bluetooth

import java.util.UUID

data class GattWriteRequest(
    val serviceUuid: UUID,
    val characteristicUuid: UUID,
    val value: ByteArray
)

enum class GattWriteDecision { APPROVED, USER_CONFIRMATION_REQUIRED, DISCONNECTED, PERMISSION_REQUIRED, STALE_SESSION, UNSUPPORTED }

/** Pure policy gate: the transport must only be called for APPROVED. */
object GattWritePolicy {
    fun evaluate(
        confirmed: Boolean,
        connected: Boolean,
        permissionGranted: Boolean,
        currentSession: Boolean,
        characteristicSupportsWrite: Boolean
    ): GattWriteDecision = when {
        !connected -> GattWriteDecision.DISCONNECTED
        !permissionGranted -> GattWriteDecision.PERMISSION_REQUIRED
        !currentSession -> GattWriteDecision.STALE_SESSION
        !characteristicSupportsWrite -> GattWriteDecision.UNSUPPORTED
        !confirmed -> GattWriteDecision.USER_CONFIRMATION_REQUIRED
        else -> GattWriteDecision.APPROVED
    }
}
