package com.dan.terminal.feature.network

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dan.terminal.core.network.AndroidConnectivityEnvironment
import com.dan.terminal.core.network.ConnectivityProbe
import com.dan.terminal.core.network.ConnectivityReportExporter
import com.dan.terminal.core.network.ConnectivitySample
import com.dan.terminal.core.network.DefaultConnectivityReportExporter
import com.dan.terminal.core.network.NetworkHistoryStore
import com.dan.terminal.core.network.PreferencesNetworkHistoryStore
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun NetworkDiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val probe = remember(context) { ConnectivityProbe(AndroidConnectivityEnvironment(context)) }
    val historyStore: NetworkHistoryStore = remember(context) { PreferencesNetworkHistoryStore(context) }
    val exporter: ConnectivityReportExporter = remember { DefaultConnectivityReportExporter() }
    var sample by remember { mutableStateOf<ConnectivitySample?>(null) }
    var history by remember { mutableStateOf<List<ConnectivitySample>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var pendingExport by rememberReportExportSelection()
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val latest = pendingExport
        if (uri == null || latest == null) return@rememberLauncherForActivityResult
        scope.launch {
            status = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { output -> exporter.writeCsv(latest, output) }
                        ?: error("Could not open the selected file")
                    "CSV report saved."
                }.getOrElse { "Export failed: ${it.message ?: "unknown error"}" }
            }
        }
    }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val latest = pendingExport
        if (uri == null || latest == null) return@rememberLauncherForActivityResult
        scope.launch {
            status = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { output -> exporter.writePdf(latest, output) }
                        ?: error("Could not open the selected file")
                    "PDF report saved."
                }.getOrElse { "Export failed: ${it.message ?: "unknown error"}" }
            }
        }
    }

    LaunchedEffect(historyStore) {
        runCatching { historyStore.load() }.onSuccess { history = it }
            .onFailure { status = "History could not be read: ${it.message}" }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Connectivity diagnostics", style = MaterialTheme.typography.headlineSmall)
        Text("User-triggered, read-only checks. TCP connection attempts are not ICMP ping or packet-loss measurements.")
        Text("Connects to 1.1.1.1:443 five times and looks up example.com. This sends network traffic; the heuristic score is not a security audit.")
        Button(
            enabled = !busy,
            onClick = {
                scope.launch {
                    busy = true
                    status = "Running five TCP connection attempts and one DNS lookup…"
                    runCatching { probe.measure() }
                        .onSuccess { measured ->
                            sample = measured
                            val saveResult = runCatching { historyStore.save(measured); history = historyStore.load() }
                            status = saveResult.fold(
                                onSuccess = { "Measurement complete." },
                                onFailure = { "Measurement complete; history save failed: ${it.message}" }
                            )
                        }
                        .onFailure { status = "Measurement failed: ${it.message ?: "unknown error"}" }
                    busy = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "Checking…" else "Run connectivity check") }

        sample?.let { current ->
            Text("${current.network} · ${current.timestampUtc}", style = MaterialTheme.typography.titleMedium)
            Text("TCP connect: ${current.tcpMs?.let { "$it ms" } ?: "Not measured"}")
            Text("DNS: ${current.dnsMs?.let { "$it ms" } ?: "Not measured"}")
            Text("Failed TCP attempts: ${current.failPct?.let { "$it%" } ?: "Not measured"}")
            Text("Score: ${current.score?.let { "$it/100 heuristic" } ?: "Not assigned"}")
            Text("${current.cause}: ${current.notes}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { pendingExport = current; exportPicker.launch("dan-connectivity.csv") }) { Text("Export CSV") }
                TextButton(onClick = { pendingExport = current; pdfPicker.launch("dan-connectivity.pdf") }) { Text("Export PDF") }
            }
        }
        status?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
        Text("Local history (${history.size})", style = MaterialTheme.typography.titleMedium)
        history.take(10).forEach { row -> Text("${row.timestampUtc} · ${row.network} · ${row.cause}") }
        TextButton(onClick = {
            scope.launch {
                runCatching { historyStore.clear(); history = emptyList() }
                    .onFailure { status = "Could not clear history: ${it.message}" }
            }
        }) { Text("Clear local history") }
        TextButton(onClick = onBack) { Text("Back") }
    }
}

// The document picker may recreate this activity before delivering its URI.
// Store the exact report chosen at launch in Android saved state.
@Composable
internal fun rememberReportExportSelection(): MutableState<ConnectivitySample?> =
    rememberSaveable { mutableStateOf<ConnectivitySample?>(null) }
