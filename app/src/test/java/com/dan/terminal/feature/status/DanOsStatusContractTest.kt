package com.dan.terminal.feature.status

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DanOsStatusContractTest {
    private val now = Instant.parse("2026-11-22T00:00:00Z")

    private fun linux(updated: Instant? = now) = DanOsLinuxObservation(
        updated = updated,
        wifi = true,
        tailscale = true,
        ssh = false,
        suspendMasked = true
    )

    @Test fun ageOfExactly120SecondsIsFresh() {
        val output = DanOsStatusContract.assess(
            linux(now.minusSeconds(120)), null, now
        )
        assertEquals(DanOsFreshness.FRESH, output.linuxRecord)
        assertEquals(true, output.wifi)
        assertEquals(false, output.ssh) // False is a real observation, not missing.
    }

    @Test fun olderObservationIsStaleAndCannotExposeCurrentStatus() {
        val output = DanOsStatusContract.assess(
            linux(now.minusSeconds(121)), null, now
        )
        assertEquals(DanOsFreshness.STALE, output.linuxRecord)
        assertNull(output.wifi)
        assertNull(output.tailscale)
        assertNull(output.ssh)
        assertNull(output.suspendMasked)
    }

    @Test fun missingLinuxObservationIsUnknown() {
        val output = DanOsStatusContract.assess(null, null, now)
        assertEquals(DanOsFreshness.UNKNOWN, output.linuxRecord)
        assertEquals(DanOsReachability.UNKNOWN, output.androidToLinux)
    }

    @Test fun absentTimestampDoesNotBecomeFresh() {
        val output = DanOsStatusContract.assess(linux(null), null, now)
        assertEquals(DanOsFreshness.UNKNOWN, output.linuxRecord)
        assertNull(output.wifi)
    }

    @Test fun futureTimestampFailsClosed() {
        val output = DanOsStatusContract.assess(linux(now.plusSeconds(1)), null, now)
        assertEquals(DanOsFreshness.UNKNOWN, output.linuxRecord)
    }

    @Test fun freshLinuxStatusIsNotProofOfPhoneConnectivity() {
        val output = DanOsStatusContract.assess(linux(), null, now)
        assertEquals(DanOsFreshness.FRESH, output.linuxRecord)
        assertEquals(DanOsReachability.UNKNOWN, output.androidToLinux)
    }

    @Test fun freshNegativePhoneProbeRemainsUnreachable() {
        val output = DanOsStatusContract.assess(
            linux(),
            DanOsPhoneReachability(now.minusSeconds(3), reachable = false),
            now
        )
        assertEquals(DanOsFreshness.FRESH, output.linuxRecord)
        assertEquals(DanOsReachability.UNREACHABLE, output.androidToLinux)
    }

    @Test fun freshPositivePhoneProbeIsIndependentOfOldLinuxRecord() {
        val output = DanOsStatusContract.assess(
            linux(now.minusSeconds(400)),
            DanOsPhoneReachability(now.minusSeconds(1), reachable = true),
            now
        )
        assertEquals(DanOsFreshness.STALE, output.linuxRecord)
        assertEquals(DanOsReachability.REACHABLE, output.androidToLinux)
        assertNull(output.wifi)
    }

    @Test fun stalePhoneProbeDoesNotCountAsReachable() {
        val output = DanOsStatusContract.assess(
            linux(),
            DanOsPhoneReachability(now.minusSeconds(121), reachable = true),
            now
        )
        assertEquals(DanOsReachability.UNKNOWN, output.androidToLinux)
    }

    @Test fun futurePhoneProbeIsUnknown() {
        val output = DanOsStatusContract.assess(
            linux(),
            DanOsPhoneReachability(now.plusSeconds(30), reachable = true),
            now
        )
        assertEquals(DanOsReachability.UNKNOWN, output.androidToLinux)
    }
}
