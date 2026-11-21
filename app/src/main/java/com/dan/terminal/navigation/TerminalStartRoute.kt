package com.dan.terminal.navigation

internal fun terminalStartRoute(setupCompleted: Boolean): String =
    if (setupCompleted) "shell" else "home"
