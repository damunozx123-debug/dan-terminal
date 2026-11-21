package com.dan.terminal.core.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class ConnectivityProbeTest {
    @Test fun noActiveNetworkProducesNoScoreOrMeasurements() = runTest {
        val probe = ConnectivityProbe(FakeEnvironment(null), FakeTransport(12.0, 25.0))

        val result = probe.measure()

        assertNull(result.score)
        assertNull(result.dnsMs)
        assertNull(result.tcpMs)
        assertEquals("NO_ACTIVE_NETWORK", result.cause)
    }

    @Test fun permissionDenialSendsZeroProbes() = runTest {
        val probe = ConnectivityProbe(
            object : ConnectivityEnvironment { override fun activeNetwork(): NetworkStatus? = throw SecurityException() },
            object : NetworkTransport {
                override fun dnsLatencyMs(): Double? = error("DNS must not run")
                override fun tcpConnectLatencyMs(): Double? = error("TCP must not run")
            }
        )
        assertEquals("PERMISSION_REQUIRED", probe.measure().cause)
    }

    @Test fun dnsFailureDoesNotInventLatencyAndKeepsTcpResult() = runTest {
        val probe = ConnectivityProbe(FakeEnvironment(NetworkStatus("Wi-Fi", -52)), FakeTransport(null, 28.0))

        val result = probe.measure()

        assertNull(result.dnsMs)
        assertEquals(28.0, result.tcpMs!!, 0.0)
        assertEquals("DNS_UNAVAILABLE", result.cause)
    }

    @Test fun totalProbeFailureHasNoHealthScore() = runTest {
        val probe = ConnectivityProbe(FakeEnvironment(NetworkStatus("Cellular", null)), FakeTransport(null, null))

        val result = probe.measure()

        assertNull(result.score)
        assertNull(result.tcpMs)
        assertEquals("INSUFFICIENT_DATA", result.cause)
    }

    @Test fun partialTcpFailuresAreReportedAsAttemptsNotIcmpLoss() = runTest {
        val probe = ConnectivityProbe(FakeEnvironment(NetworkStatus("Wi-Fi", null)), FakeTransport(20.0, null, 30.0, 30.0, 30.0, 30.0))

        val result = probe.measure()

        assertEquals(20.0, result.failPct!!, 0.0)
        assertEquals(30.0, result.tcpMs!!, 0.0)
        assertEquals(0.0, result.jitterMs!!, 0.0)
    }

    @Test fun successfulProbeHasBoundedScoreAndRetainsRssi() = runTest {
        val probe = ConnectivityProbe(FakeEnvironment(NetworkStatus("Wi-Fi", -50)), FakeTransport(22.0, 30.0))

        val result = probe.measure()

        assertEquals(22.0, result.dnsMs!!, 0.0)
        assertEquals(30.0, result.tcpMs!!, 0.0)
        assertEquals(-50, result.wifiRssiDbm)
        assertTrue(result.score != null && result.score in 0..100)
    }

    @Test fun csvQuotesFieldsAndNeutralizesSpreadsheetFormulas() {
        val output = ByteArrayOutputStream()
        DefaultConnectivityReportExporter().writeCsv(
            ConnectivitySample(network = "Wi-Fi, guest", cause = "@SUM(A1:A2)", notes = "say \"hi\""), output
        )

        val csv = output.toString(Charsets.UTF_8.name())
        assertTrue(csv.contains("\"network\",\"Wi-Fi, guest\""))
        assertTrue(csv.contains("\"cause\",\"'@SUM(A1:A2)\""))
        assertTrue(csv.contains("\"notes\",\"say \"\"hi\"\"\""))

        val formula = ByteArrayOutputStream()
        DefaultConnectivityReportExporter().writeCsv(
            ConnectivitySample(network = "Wi-Fi", cause = "  =1+1", notes = ""), formula
        )
        assertTrue(formula.toString(Charsets.UTF_8.name()).contains("\"cause\",\"'  =1+1\""))
    }

    private class FakeEnvironment(private val status: NetworkStatus?) : ConnectivityEnvironment {
        override fun activeNetwork(): NetworkStatus? = status
    }

    private class FakeTransport(private val dns: Double?, private vararg val tcpValues: Double?) : NetworkTransport {
        private var attempt = 0
        override fun dnsLatencyMs(): Double? = dns
        override fun tcpConnectLatencyMs(): Double? {
            val value = tcpValues[attempt % tcpValues.size]
            attempt += 1
            return value
        }
    }
}
