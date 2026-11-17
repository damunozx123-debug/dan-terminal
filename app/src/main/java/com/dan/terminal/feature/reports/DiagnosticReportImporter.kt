package com.dan.terminal.feature.reports

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Read-only, version-pinned import boundary. A valid report is still UNTRUSTED.
 * The JSON Schema v1.0.0 asset documents this validator's wire contract.
 * No scan execution or agent credentials may cross this boundary.
 */
object DiagnosticReportImporter {
    const val MIME = "application/vnd.dan.diagnostic-report+json"
    const val MAX_BYTES = 65536
    private val reportFields = setOf("schema_version", "report_id", "report_type", "producer",
        "created_at_utc", "measurement_origin", "status", "measurements", "findings", "limitations")
    private val measurementFields = setOf("id", "label", "method", "unit", "status", "value", "reason_code")
    private val statuses = setOf("MEASURED", "UNMEASURED", "DENIED", "UNSUPPORTED", "TIMEOUT", "ERROR")
    private val reasons = setOf("NOT_RECORDED", "PERMISSION_DENIED", "PLATFORM_UNSUPPORTED",
        "DEADLINE_EXCEEDED", "MEASUREMENT_FAILED")
    private val units = setOf("ms", "dBm", "percent", "count", "none")
    private val methods = setOf("tcp_connect", "dns_lookup", "android_wifi", "derived")
    private val uuid = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
    private val shortId = Regex("[a-z][a-z0-9_]{1,63}")
    private val code = Regex("[A-Z][A-Z0-9_]{1,63}")
    private val utc = Regex("\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d(?:\\.\\d{1,9})?Z")

    data class Preview(val report: JSONObject, val bytes: ByteArray) {
        val id: String get() = report.getString("report_id")
        val origin: String get() = report.getString("measurement_origin")
        val status: String get() = report.getString("status")
    }

    fun preview(context: Context, uri: Uri): Preview {
        require(uri.scheme == "content") { "Only shared content:// reports are accepted" }
        val data = ByteArrayOutputStream()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val buffer = ByteArray(4096)
            while (true) {
                val n = stream.read(buffer)
                if (n == -1) break
                require(data.size() + n <= MAX_BYTES) { "Report exceeds 64 KiB" }
                data.write(buffer, 0, n)
            }
        } ?: throw IllegalArgumentException("Report URI is unavailable or permission expired")
        return parseValidatedBytes(data.toByteArray())
    }

    private fun parseValidatedBytes(bytes: ByteArray): Preview {
        require(bytes.isNotEmpty() && bytes.size <= MAX_BYTES) { "Empty or oversized report" }
        val text = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes)).toString()
        enforceNestingDepth(text)
        val report = JSONObject(text)
        validate(report)
        return Preview(report, bytes)
    }

    private fun keys(obj: JSONObject): Set<String> = obj.keys().asSequence().toSet()
    private fun exact(obj: JSONObject, required: Set<String>, allowed: Set<String> = required) {
        val present = keys(obj)
        require(present.containsAll(required) && allowed.containsAll(present)) {
            "Missing or unsupported fields in diagnostic report"
        }
    }
    private fun string(obj: JSONObject, field: String, max: Int = 2048): String {
        val value = obj.get(field)
        require(value is String && value.isNotEmpty() && value.length <= max) {
            "Invalid diagnostic field: $field"
        }
        return value
    }
    private fun array(obj: JSONObject, field: String, max: Int): JSONArray {
        val value = obj.getJSONArray(field)
        require(value.length() <= max) { "Too many $field entries" }
        return value
    }
    private fun checkCode(value: String) = require(code.matches(value)) { "Invalid finding or limitation code" }

    fun validate(report: JSONObject) {
        exact(report, reportFields)
        require(string(report, "schema_version") == "1.0.0") { "Unsupported report schema version" }
        require(uuid.matches(string(report, "report_id", 36))) { "Invalid report identifier" }
        require(string(report, "report_type") in setOf("connectivity", "wifi_audit", "lan_inventory", "ble_audit")) {
            "Unsupported diagnostic type"
        }
        val producer = report.getJSONObject("producer")
        exact(producer, setOf("app_id", "app_version"))
        string(producer, "app_id", 128)
        string(producer, "app_version", 40)
        val timestamp = string(report, "created_at_utc", 40)
        require(utc.matches(timestamp)) { "Invalid UTC timestamp" }
        Instant.parse(timestamp)
        require(string(report, "measurement_origin") in setOf("LIVE", "SIMULATED"))
        require(string(report, "status") in setOf("COMPLETE", "PARTIAL", "FAILED", "INSUFFICIENT_DATA"))

        val measurements = array(report, "measurements", 128)
        val ids = mutableSetOf<String>()
        for (i in 0 until measurements.length()) {
            val m = measurements.getJSONObject(i)
            exact(m, setOf("id", "label", "method", "unit", "status"), measurementFields)
            val id = string(m, "id", 64)
            require(shortId.matches(id) && ids.add(id)) { "Invalid or duplicate measurement ID" }
            string(m, "label", 100)
            require(string(m, "method") in methods) { "Unsupported measurement method" }
            require(string(m, "unit") in units) { "Unsupported measurement unit" }
            val status = string(m, "status")
            require(status in statuses) { "Invalid measurement status" }
            if (status == "MEASURED") {
                require(m.has("value") && !m.has("reason_code")) { "Measured value missing" }
                val value = m.get("value")
                require(value is Number && value.toDouble().isFinite()) { "Non-finite measurement" }
            } else {
                require(!m.has("value") && m.has("reason_code")) { "Unavailable metric requires a reason" }
                require(string(m, "reason_code") in reasons) { "Invalid missing-data reason" }
            }
        }

        val findings = array(report, "findings", 64)
        val findingIds = mutableSetOf<String>()
        for (i in 0 until findings.length()) {
            val finding = findings.getJSONObject(i)
            exact(finding, setOf("id", "code", "message", "supporting_measurement_ids"))
            val id = string(finding, "id", 64)
            require(shortId.matches(id) && findingIds.add(id)) { "Duplicate finding ID" }
            checkCode(string(finding, "code", 64))
            string(finding, "message")
            val refs = array(finding, "supporting_measurement_ids", 128)
            require(refs.length() > 0) { "Finding has no supporting measurements" }
            val seen = mutableSetOf<String>()
            for (j in 0 until refs.length()) {
                val referencedId = refs.getString(j)
                require(referencedId in ids && seen.add(referencedId)) { "Invalid supporting measurement reference" }
            }
        }

        val limits = array(report, "limitations", 64)
        for (i in 0 until limits.length()) {
            val limit = limits.getJSONObject(i)
            exact(limit, setOf("code", "message"))
            checkCode(string(limit, "code", 64))
            string(limit, "message", 1024)
        }
    }

    /** Reject excessive nesting before invoking JSONObject's recursive parser. */
    private fun enforceNestingDepth(text: String) {
        var depth = 0
        var quoted = false
        var escaped = false
        for (char in text) {
            if (quoted) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> quoted = false
                }
            } else when (char) {
                '"' -> quoted = true
                '{', '[' -> { depth++; require(depth <= 16) { "Report nesting exceeds limit" } }
                '}', ']' -> depth--
            }
            require(depth >= 0) { "Malformed report JSON" }
        }
        require(depth == 0 && !quoted) { "Malformed report JSON" }
    }

    /** Only canonical UUID filenames are ever opened or deleted from private storage. */
    fun listImportedIds(context: Context): List<String> {
        val dir = File(context.filesDir, "imported_diagnostics")
        return dir.listFiles().orEmpty()
            .filter { it.isFile && it.name.endsWith(".json") &&
                uuid.matches(it.name.removeSuffix(".json")) }
            .sortedByDescending { it.lastModified() }
            .take(100)
            .map { it.name.removeSuffix(".json") }
    }

    fun openImported(context: Context, reportId: String): Preview {
        require(uuid.matches(reportId)) { "Invalid report ID" }
        val file = File(File(context.filesDir, "imported_diagnostics"), "$reportId.json")
        require(file.isFile && file.length() in 1..MAX_BYTES.toLong()) { "Saved report unavailable" }
        return parseValidatedBytes(file.readBytes())
    }

    @Synchronized
    fun deleteImported(context: Context, reportId: String): Boolean {
        require(uuid.matches(reportId)) { "Invalid report ID" }
        val file = File(File(context.filesDir, "imported_diagnostics"), "$reportId.json")
        return !file.exists() || file.delete()
    }

    /** Idempotent app-private import. No API endpoint or shared database. */
    @Synchronized
    fun import(context: Context, preview: Preview): String {
        val dir = File(context.filesDir, "imported_diagnostics")
        require(dir.isDirectory || dir.mkdirs()) { "Cannot create import directory" }
        val output = File(dir, "${preview.id}.json")
        if (output.exists()) {
            if (output.readBytes().contentEquals(preview.bytes)) return "Already imported (identical)"
            throw IllegalArgumentException("Report ID exists with different content")
        }
        require(dir.listFiles()?.size ?: 0 < 100) { "Report storage limit reached" }
        val tmp = File.createTempFile("incoming-", ".tmp", dir)
        try {
            FileOutputStream(tmp).use { stream ->
                stream.write(preview.bytes)
                stream.fd.sync()
            }
            require(tmp.renameTo(output)) { "Could not save report" }
            return "Imported locally — authorship unverified"
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }
}
