package com.dan.terminal.feature.termux

data class TermuxCommandSpec(
    val executablePath: String,
    val arguments: List<String>
)

object TermuxRunCommandContract {
    const val BASH_EXECUTABLE = "/data/data/com.termux/files/usr/bin/bash"
    // Termux PendingIntent result bundles use camelCase, including v0.118.0.
    const val RESULT_EXIT_CODE = "exitCode"

    fun shell(command: String): TermuxCommandSpec =
        TermuxCommandSpec(
            executablePath = BASH_EXECUTABLE,
            arguments = listOf("-c", command)
        )
}
