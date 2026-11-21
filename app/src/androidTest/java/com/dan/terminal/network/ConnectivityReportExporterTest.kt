package com.dan.terminal.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dan.terminal.core.network.ConnectivitySample
import com.dan.terminal.core.network.DefaultConnectivityReportExporter
import com.dan.terminal.core.network.PreferencesNetworkHistoryStore
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class ConnectivityReportExporterTest {
    @Test fun pdfExporterWritesAReadablePdfDocument() {
        val output = ByteArrayOutputStream()
        DefaultConnectivityReportExporter().writePdf(
            ConnectivitySample(network = "Wi-Fi", cause = "NO_MAJOR_ISSUE_OBSERVED", notes = "No major issue observed."),
            output
        )

        assertTrue(output.size() > 100)
        assertTrue(output.toByteArray().copyOfRange(0, 5).contentEquals("%PDF-".toByteArray()))
    }

    @Test fun longPdfNotesArePaginatedAndCanBeOpenedByAndroid() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("network-report-", ".pdf", context.cacheDir)
        try {
            file.outputStream().use { output ->
                DefaultConnectivityReportExporter().writePdf(
                    ConnectivitySample(network = "Wi-Fi", cause = "INSUFFICIENT_DATA", notes = "A long observation. ".repeat(1000)), output
                )
            }
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                PdfRenderer(descriptor).use { renderer -> assertTrue(renderer.pageCount > 1) }
            }
        } finally { file.delete() }
    }

    @Test fun historyRetains100NewestSamplesAndPreservesUnavailableValues() = runBlocking {
        val store = PreferencesNetworkHistoryStore(InstrumentationRegistry.getInstrumentation().targetContext)
        store.clear()
        try {
            repeat(101) { index ->
                store.save(ConnectivitySample(timestampUtc = index.toString(), network = "Wi-Fi", cause = "INSUFFICIENT_DATA", notes = "No TCP result"))
            }
            val history = store.load()
            assertEquals(100, history.size)
            assertEquals("100", history.first().timestampUtc)
            assertEquals("1", history.last().timestampUtc)
            assertTrue(history.all { it.tcpMs == null && it.score == null })
            store.clear()
            assertTrue(store.load().isEmpty())
        } finally { store.clear() }
    }
}
