package com.dan.terminal.network

import androidx.compose.runtime.MutableState
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.dan.terminal.core.network.ConnectivitySample
import com.dan.terminal.feature.network.rememberReportExportSelection
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReportExportSelectionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun reportChosenForCsvOrPdfSurvivesPickerActivityRecreation() {
        val restoration = StateRestorationTester(compose)
        lateinit var selected: MutableState<ConnectivitySample?>
        restoration.setContent { selected = rememberReportExportSelection() }
        val report = ConnectivitySample(network = "Wi-Fi", timestampUtc = "2026-11-15T00:00:00Z", cause = "INSUFFICIENT_DATA", notes = "Selected before opening either document picker")
        compose.runOnIdle { selected.value = report }
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle { assertEquals(report, selected.value) }
    }
}
