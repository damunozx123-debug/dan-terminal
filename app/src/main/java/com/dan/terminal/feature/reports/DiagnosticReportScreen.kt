package com.dan.terminal.feature.reports

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Read-only consumer UI. The user must explicitly confirm the private import. */
@Composable
fun DiagnosticReportScreen(incomingUri: Uri?, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember(incomingUri) { mutableStateOf(incomingUri) }
    var preview by remember(selectedUri) { mutableStateOf<DiagnosticReportImporter.Preview?>(null) }
    var error by remember(selectedUri) { mutableStateOf<String?>(null) }
    var notice by remember(selectedUri) { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selectedUri = uri
    }

    LaunchedEffect(selectedUri) {
        val uri = selectedUri ?: return@LaunchedEffect
        try {
            preview = withContext(Dispatchers.IO) {
                DiagnosticReportImporter.preview(context.applicationContext, uri)
            }
            error = null
        } catch (failure: Exception) {
            preview = null
            error = failure.message ?: "Cannot read or validate this report"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Diagnostic reports", style = MaterialTheme.typography.headlineMedium)
        Text("Imported reports are unverified third-party data, even if their JSON is valid.")
        Button(
            onClick = { picker.launch(arrayOf(DiagnosticReportImporter.MIME, "application/json")) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Choose report file") }
        error?.let { Text("Import blocked: $it", color = MaterialTheme.colorScheme.error) }
        notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        preview?.let { item ->
            val report = item.report
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Imported / UNVERIFIED AUTHORSHIP", style = MaterialTheme.typography.titleMedium)
                    Text("Origin asserted by source: ${item.origin}")
                    Text("Assessment status: ${item.status}")
                    Text("Type: ${report.getString("report_type")}")
                    Text("Producer claim: ${report.getJSONObject("producer").getString("app_id")}")
                    Text("Recorded UTC (clock unverified): ${report.getString("created_at_utc")}")
                    Text("Report ID: ${item.id}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("Measurements", style = MaterialTheme.typography.titleMedium)
            val measurements = report.getJSONArray("measurements")
            for (i in 0 until measurements.length()) {
                val m = measurements.getJSONObject(i)
                val status = m.getString("status")
                val valueText = if (status == "MEASURED") {
                    "${m.getDouble("value")} ${m.getString("unit")} (${m.getString("method")})"
                } else {
                    "$status / ${m.getString("reason_code")}"
                }
                Text("${m.getString("label")}: $valueText")
            }
            val findings = report.getJSONArray("findings")
            if (findings.length() > 0) Text("Findings (heuristic observations)", style = MaterialTheme.typography.titleMedium)
            for (i in 0 until findings.length()) {
                val finding = findings.getJSONObject(i)
                Text("${finding.getString("code")}: ${finding.getString("message")}")
                Text("Supporting metric IDs: ${finding.getJSONArray("supporting_measurement_ids")}", style = MaterialTheme.typography.bodySmall)
            }
            Text("Limitations", style = MaterialTheme.typography.titleMedium)
            val limits = report.getJSONArray("limitations")
            for (i in 0 until limits.length()) {
                val limit = limits.getJSONObject(i)
                Text("${limit.getString("code")}: ${limit.getString("message")}")
            }
            Button(
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    saving = true
                    scope.launch {
                        try {
                            notice = withContext(Dispatchers.IO) {
                                DiagnosticReportImporter.import(context.applicationContext, item)
                            }
                        } catch (failure: Exception) {
                            error = failure.message ?: "Import failed"
                        } finally {
                            saving = false
                        }
                    }
                }
            ) { Text("Confirm import to DAN Terminal") }
        }
        TextButton(onClick = onBack) { Text("Back") }
    }
}
