package com.dan.terminal.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.Manifest
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.roundToInt

data class ConnectivitySample(
    val source: String = "LIVE",
    val timestampUtc: String = java.time.Instant.now().toString(),
    val network: String,
    val tcpMs: Double? = null,
    val dnsMs: Double? = null,
    val failPct: Double? = null,
    val jitterMs: Double? = null,
    val wifiRssiDbm: Int? = null,
    val score: Int? = null,
    val cause: String,
    val notes: String
) : java.io.Serializable

data class NetworkStatus(val label: String, val wifiRssiDbm: Int?)

interface ConnectivityEnvironment {
    fun activeNetwork(): NetworkStatus?
}

interface NetworkTransport {
    fun dnsLatencyMs(): Double?
    fun tcpConnectLatencyMs(): Double?
}

/** Runs all blocking socket and DNS work on [ioDispatcher], never on the caller's UI thread. */
class ConnectivityProbe(
    private val environment: ConnectivityEnvironment,
    private val transport: NetworkTransport = SocketNetworkTransport(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun measure(): ConnectivitySample = withContext(ioDispatcher) {
        val network = try { environment.activeNetwork() } catch (_: SecurityException) {
            return@withContext ConnectivitySample(network = "Unavailable", cause = "PERMISSION_REQUIRED",
                notes = "Network access permission is unavailable. No probes were sent.")
        }
            ?: return@withContext ConnectivitySample(
                network = "Unavailable",
                cause = "NO_ACTIVE_NETWORK",
                notes = "Android reported no active network. No probe score was assigned."
            )
        currentCoroutineContext().ensureActive()
        val dns = runCatching { transport.dnsLatencyMs() }.getOrNull()?.takeIf { it.isFinite() && it >= 0.0 }
        val attempts = (1..5).map {
            currentCoroutineContext().ensureActive()
            runCatching { transport.tcpConnectLatencyMs() }.getOrNull()?.takeIf { ms -> ms.isFinite() && ms >= 0.0 }
        }
        val connected = attempts.filterNotNull()
        val tcp = connected.takeIf { it.isNotEmpty() }?.average()
        val failPct = ((attempts.size - connected.size) * 100.0 / attempts.size)
        val jitter = connected.zipWithNext { first, second -> kotlin.math.abs(second - first) }
            .takeIf { it.isNotEmpty() }?.average()
        val score = tcp?.let { (100 - (it / 4).roundToInt() - (failPct * 0.55).roundToInt()).coerceIn(0, 100) }
        val cause = when {
            tcp == null -> "INSUFFICIENT_DATA"
            failPct >= 40.0 -> "INTERMITTENT_TCP_REACHABILITY"
            dns == null -> "DNS_UNAVAILABLE"
            tcp > 120.0 -> "HIGH_TCP_CONNECT_LATENCY"
            else -> "NO_MAJOR_ISSUE_OBSERVED"
        }
        ConnectivitySample(
            network = network.label,
            tcpMs = tcp,
            dnsMs = dns,
            failPct = failPct,
            jitterMs = jitter,
            wifiRssiDbm = network.wifiRssiDbm,
            score = if (tcp == null) null else score,
            cause = cause,
            notes = when (cause) {
                "NO_MAJOR_ISSUE_OBSERVED" -> "No major issue observed in this short read-only test. This does not rule out intermittent faults."
                "HIGH_TCP_CONNECT_LATENCY" -> "TCP connection setup was slow. This is not an ICMP ping or packet-loss test."
                "INTERMITTENT_TCP_REACHABILITY" -> "Some TCP connection attempts failed. This is not an ICMP packet-loss measurement."
                "DNS_UNAVAILABLE" -> "DNS lookup was unavailable. The TCP result is retained without a DNS value."
                else -> "No successful TCP measurement. This may be endpoint filtering or an outage; no health score was assigned."
            }
        )
    }
}

class AndroidConnectivityEnvironment(context: Context) : ConnectivityEnvironment {
    private val appContext = context.applicationContext

    override fun activeNetwork(): NetworkStatus? {
        val manager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        val active = manager.activeNetwork ?: return null
        val caps = manager.getNetworkCapabilities(active) ?: return null
        val label = when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Other Android network"
        }
        val rssi = if (label == "Wi-Fi" && appContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            runCatching {
                (appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager)?.connectionInfo?.rssi
                    ?.takeIf { it in -126..-1 }
            }.getOrNull()
        } else null
        return NetworkStatus(label, rssi)
    }
}

class SocketNetworkTransport(
    private val host: String = "example.com",
    private val address: String = "1.1.1.1",
    private val port: Int = 443,
    private val timeoutMs: Int = 2400
) : NetworkTransport {
    override fun dnsLatencyMs(): Double? = measureMs { InetAddress.getAllByName(host).isNotEmpty() }

    override fun tcpConnectLatencyMs(): Double? = measureMs {
        Socket().use { it.connect(InetSocketAddress(address, port), timeoutMs) }
    }

    private fun measureMs(block: () -> Any?): Double? {
        val started = System.nanoTime()
        return try {
            block()
            ((System.nanoTime() - started) / 100_000.0).roundToInt() / 10.0
        } catch (_: Exception) {
            null
        }
    }
}
