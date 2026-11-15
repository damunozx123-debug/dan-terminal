package com.dan.terminal.core.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class GattWritePolicyTest {
    @Test fun deniedConfirmationCannotReachTransport() {
        val transport = FakeTransport()
        val decision = GattWritePolicy.evaluate(
            confirmed = false, connected = true, permissionGranted = true,
            currentSession = true, characteristicSupportsWrite = true
        )
        if (decision == GattWriteDecision.APPROVED) transport.write(GattWriteRequest(UUID.randomUUID(), UUID.randomUUID(), byteArrayOf(1)))

        assertEquals(GattWriteDecision.USER_CONFIRMATION_REQUIRED, decision)
        assertEquals(0, transport.writeCount)
    }

    @Test fun deniedPermissionOrStaleSessionCannotBeApproved() {
        assertEquals(GattWriteDecision.PERMISSION_REQUIRED, GattWritePolicy.evaluate(true, true, false, true, true))
        assertEquals(GattWriteDecision.STALE_SESSION, GattWritePolicy.evaluate(true, true, true, false, true))
        assertEquals(GattWriteDecision.UNSUPPORTED, GattWritePolicy.evaluate(true, true, true, true, false))
    }

    @Test fun validConfirmedWriteCanPassPolicy() {
        assertEquals(GattWriteDecision.APPROVED, GattWritePolicy.evaluate(true, true, true, true, true))
    }

    private class FakeTransport {
        var writeCount = 0
        fun write(request: GattWriteRequest) { writeCount++ }
    }
}
