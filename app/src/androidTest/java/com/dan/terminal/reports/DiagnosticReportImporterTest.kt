package com.dan.terminal.reports

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dan.terminal.feature.reports.DiagnosticReportImporter
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.nio.charset.StandardCharsets
import java.util.UUID

/** Emulator instrumentation: checks report validation and app-private import only. */
@RunWith(AndroidJUnit4::class)
class DiagnosticReportImporterTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun fixture(name: String = "simulated-connectivity.json"): JSONObject =
        context.assets.open("diagnostics/fixtures/$name").bufferedReader(StandardCharsets.UTF_8).use {
            JSONObject(it.readText())
        }

    private fun rejected(block: () -> Unit) {
        try {
            block()
            fail("Expected diagnostic report rejection")
        } catch (_: IllegalArgumentException) {
            // Strict schema, authorization or identity check.
        } catch (_: org.json.JSONException) {
            // Missing or mistyped nested JSON field.
        }
    }

    @Test fun syntheticFixturesValidateWithoutClaimingLiveEvidence() {
        for (name in listOf("simulated-connectivity.json", "missing-connectivity.json")) {
            val report = fixture(name)
            DiagnosticReportImporter.validate(report)
            assertEquals("SIMULATED", report.getString("measurement_origin"))
        }
    }

    @Test fun rejectsUnknownFieldsAndUnsupportedMethods() {
        val secret = fixture().apply { put("pairing_token", "not-allowed") }
        rejected { DiagnosticReportImporter.validate(secret) }
        val wrongMethod = fixture()
        wrongMethod.getJSONArray("measurements").getJSONObject(0).put("method", "icmp_loss")
        rejected { DiagnosticReportImporter.validate(wrongMethod) }
    }

    @Test fun rejectsMissingMeasurementValuesAndBrokenEvidenceLinks() {
        val missing = fixture()
        missing.getJSONArray("measurements").getJSONObject(0).remove("value")
        rejected { DiagnosticReportImporter.validate(missing) }
        val missingRef = fixture()
        missingRef.getJSONArray("findings").getJSONObject(0)
            .getJSONArray("supporting_measurement_ids").put("unknown_id")
        rejected { DiagnosticReportImporter.validate(missingRef) }
    }

    @Test fun rejectsUnsupportedUriSchemesBeforeOpeningAStream() {
        rejected {
            DiagnosticReportImporter.preview(context, Uri.parse("https://example.invalid/test.json"))
        }
    }

    @Test fun savesIdenticalReportIdempotentlyAndRejectsConflictingContent() {
        val id = UUID.randomUUID().toString()
        val report = fixture().apply { put("report_id", id) }
        val original = report.toString().toByteArray(StandardCharsets.UTF_8)
        DiagnosticReportImporter.validate(report)
        val first = DiagnosticReportImporter.Preview(report, original)
        try {
            assertTrue(DiagnosticReportImporter.import(context, first).contains("Imported locally"))
            assertTrue(DiagnosticReportImporter.import(context, first).contains("Already imported"))
            assertTrue(DiagnosticReportImporter.listImportedIds(context).contains(id))
            assertEquals(id, DiagnosticReportImporter.openImported(context, id).id)

            val conflict = JSONObject(report.toString())
            conflict.getJSONArray("limitations").getJSONObject(0)
                .put("message", "Changed report with an identical report ID.")
            DiagnosticReportImporter.validate(conflict)
            rejected {
                DiagnosticReportImporter.import(
                    context,
                    DiagnosticReportImporter.Preview(
                        conflict,
                        conflict.toString().toByteArray(StandardCharsets.UTF_8)
                    )
                )
            }
        } finally {
            assertTrue(DiagnosticReportImporter.deleteImported(context, id))
            assertFalse(DiagnosticReportImporter.listImportedIds(context).contains(id))
        }
    }

    @Test fun rejectsMutatedPreviewIdentityAtSaveBoundary() {
        val id = UUID.randomUUID().toString()
        val report = fixture().apply { put("report_id", id) }
        val original = report.toString().toByteArray(StandardCharsets.UTF_8)
        val preview = DiagnosticReportImporter.Preview(report, original)
        report.put("report_id", UUID.randomUUID().toString())
        rejected { DiagnosticReportImporter.import(context, preview) }
        assertFalse(DiagnosticReportImporter.listImportedIds(context).contains(id))
    }
}
