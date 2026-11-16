package com.dan.terminal

import androidx.compose.ui.unit.Density
import com.dan.terminal.feature.termux.terminalFontSizePixels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TerminalFontSizeTest {
    @Test fun scalesTextForHighDensityPhoneInsteadOfUsingRawPixels() {
        // A 14sp terminal on a 3x screen needs 42 physical pixels, not 16.
        assertEquals(42, terminalFontSizePixels(14, Density(3f, 1f)))
    }

    @Test fun respectsAccessibilityFontScale() {
        // Android's large-font curve is non-linear. It must enlarge the
        // 28px baseline; do not replace the platform curve with raw multiplication.
        assertTrue(terminalFontSizePixels(14, Density(2f, 1.5f)) > 28)
    }

    @Test fun keepsUserSelectedSizeOnStandardDensity() {
        assertEquals(18, terminalFontSizePixels(18, Density(1f, 1f)))
    }

    @Test fun roundsHalfPixelUpOnFractionalDensityScreens() {
        assertEquals(39, terminalFontSizePixels(14, Density(2.75f, 1f)))
    }
}
