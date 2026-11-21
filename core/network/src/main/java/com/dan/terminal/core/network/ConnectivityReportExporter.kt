package com.dan.terminal.core.network

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import java.nio.charset.StandardCharsets

interface ConnectivityReportExporter {
    fun writeCsv(sample: ConnectivitySample, output: OutputStream)
    fun writePdf(sample: ConnectivitySample, output: OutputStream)
}

class DefaultConnectivityReportExporter : ConnectivityReportExporter {
    override fun writeCsv(sample: ConnectivitySample, output: OutputStream) {
        val rows = listOf(
            "source" to sample.source,
            "timestamp_utc" to sample.timestampUtc,
            "network" to sample.network,
            "tcp_connect_ms" to sample.tcpMs?.toString(),
            "dns_ms" to sample.dnsMs?.toString(),
            "tcp_connect_fail_pct" to sample.failPct?.toString(),
            "connect_time_variation_ms" to sample.jitterMs?.toString(),
            "wifi_rssi_dbm" to sample.wifiRssiDbm?.toString(),
            "score" to sample.score?.toString(),
            "cause" to sample.cause,
            "notes" to sample.notes
        )
        val csv = buildString {
            append("field,value\r\n")
            rows.forEach { (key, value) -> append(escape(key)).append(',').append(escape(value.orEmpty())).append("\r\n") }
        }
        output.write(csv.toByteArray(StandardCharsets.UTF_8))
    }

    override fun writePdf(sample: ConnectivitySample, output: OutputStream) {
        val document = PdfDocument()
        try {
            var pageNumber = 1
            var page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 13f }
            var y = 50f
            val rows = listOf(
                "DAN Terminal - Read-only connectivity check",
                "Time: ${sample.timestampUtc}", "Network: ${sample.network}",
                "TCP connect: ${sample.tcpMs?.let { "$it ms" } ?: "Not measured"}",
                "DNS: ${sample.dnsMs?.let { "$it ms" } ?: "Not measured"}",
                "Attempt failures: ${sample.failPct?.let { "$it%" } ?: "Not measured"}",
                "Wi-Fi RSSI: ${sample.wifiRssiDbm?.let { "$it dBm" } ?: "Not measured"}",
                "Score: ${sample.score?.let { "$it/100 heuristic" } ?: "Not assigned"}",
                "Classification: ${sample.cause}", "Notes: ${sample.notes}",
                "TCP connect attempts are not ICMP packet-loss measurements.",
                "Score and classification are unvalidated heuristics, not proof of general network health."
            )
            rows.forEach { text ->
                text.lines().forEach { line ->
                    var remaining = line
                    do {
                        if (y > 790f) {
                            document.finishPage(page)
                            pageNumber += 1
                            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                            y = 50f
                        }
                        val count = if (remaining.isEmpty()) 0 else paint.breakText(remaining, true, 515f, null).coerceAtLeast(1)
                        page.canvas.drawText(remaining.take(count), 40f, y, paint)
                        remaining = remaining.drop(count)
                        y += 19f
                    } while (remaining.isNotEmpty())
                }
                y += 8f
            }
            document.finishPage(page)
            document.writeTo(output)
        } finally {
            document.close()
        }
    }

    private fun escape(value: String): String {
        val firstMeaningfulCharacter = value.firstOrNull { !it.isWhitespace() }
        val safe = if (firstMeaningfulCharacter in listOf('=', '+', '-', '@')) "'$value" else value
        return "\"${safe.replace("\"", "\"\"")}\""
    }
}
