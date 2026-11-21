package com.dan.terminal.feature.termux

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp

internal const val DEFAULT_TERMINAL_FONT_SP = 14
internal const val MIN_TERMINAL_FONT_SP = 10
internal const val MAX_TERMINAL_FONT_SP = 28

/** TerminalRenderer passes its size straight to Paint, so it needs physical pixels. */
internal fun terminalFontSizePixels(sizeSp: Int, density: Density): Int = with(density) {
    sizeSp.coerceIn(MIN_TERMINAL_FONT_SP, MAX_TERMINAL_FONT_SP).sp.roundToPx()
}
