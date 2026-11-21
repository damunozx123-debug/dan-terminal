package com.dan.terminal.core.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

interface NetworkHistoryStore {
    suspend fun save(sample: ConnectivitySample)
    suspend fun load(): List<ConnectivitySample>
    suspend fun clear()
}

class PreferencesNetworkHistoryStore(context: Context) : NetworkHistoryStore {
    private val prefs = context.applicationContext.getSharedPreferences("dan_terminal_network_history", Context.MODE_PRIVATE)

    override suspend fun save(sample: ConnectivitySample) {
        withContext(Dispatchers.IO) {
            lock.withLock {
                val array = JSONArray().put(sample.toJson())
                read().take(99).forEach { array.put(it.toJson()) }
                check(prefs.edit().putString(KEY, array.toString()).commit()) { "Unable to persist diagnostic history" }
            }
        }
    }

    override suspend fun load(): List<ConnectivitySample> = withContext(Dispatchers.IO) {
        lock.withLock { read() }
    }

    private fun read(): List<ConnectivitySample> {
        val rows = JSONArray(prefs.getString(KEY, "[]") ?: "[]")
        return (0 until rows.length()).map { rows.getJSONObject(it).toSample() }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        lock.withLock {
            check(prefs.edit().remove(KEY).commit()) { "Unable to clear diagnostic history" }
        }
    }

    private fun ConnectivitySample.toJson() = JSONObject()
        .put("source", source).put("timestampUtc", timestampUtc).put("network", network)
        .putNullable("tcpMs", tcpMs).putNullable("dnsMs", dnsMs).putNullable("failPct", failPct)
        .putNullable("jitterMs", jitterMs).putNullable("wifiRssiDbm", wifiRssiDbm)
        .putNullable("score", score).put("cause", cause).put("notes", notes)

    private fun JSONObject.putNullable(key: String, value: Number?) = put(key, value ?: JSONObject.NULL)

    private fun JSONObject.toSample() = ConnectivitySample(
        source = optString("source", "LIVE"), timestampUtc = optString("timestampUtc", ""),
        network = optString("network", "Unknown"), tcpMs = optNullableDouble("tcpMs"),
        dnsMs = optNullableDouble("dnsMs"), failPct = optNullableDouble("failPct"),
        jitterMs = optNullableDouble("jitterMs"), wifiRssiDbm = optNullableInt("wifiRssiDbm"),
        score = optNullableInt("score"), cause = optString("cause", "INSUFFICIENT_DATA"),
        notes = optString("notes", "")
    )

    private fun JSONObject.optNullableDouble(key: String) = if (isNull(key)) null else optDouble(key).takeIf { it.isFinite() }
    private fun JSONObject.optNullableInt(key: String) = if (isNull(key)) null else optInt(key)

    private companion object {
        const val KEY = "samples"
        val lock = Mutex()
    }
}
