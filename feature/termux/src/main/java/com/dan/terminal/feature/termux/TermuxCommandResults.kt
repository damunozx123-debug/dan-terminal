package com.dan.terminal.feature.termux

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger

enum class CommandState { RUNNING, FINISHED, FAILED }

data class CommandEntry(
    val id: Int,
    val command: String,
    val state: CommandState,
    val stdout: String = "",
    val stderr: String = "",
    val exitCode: Int? = null,
    val error: String? = null
)

/**
 * In-memory command transcript only: secrets in terminal output are not written to disk.
 * Results can arrive asynchronously from Termux's PendingIntent after navigating away.
 * This is a one-shot command runner, not a persistent PTY or streaming shell.
 */
object TermuxCommandResults {
    private const val MAX_ENTRIES = 20
    private const val MAX_OUTPUT_CHARS = 32_000
    private val ids = AtomicInteger(100)
    private val mutableEntries = MutableStateFlow<List<CommandEntry>>(emptyList())
    val entries = mutableEntries.asStateFlow()

    @Synchronized
    fun start(command: String): Int? {
        if (command.isBlank() || command.length > 4096) return null
        if (mutableEntries.value.any { it.state == CommandState.RUNNING }) return null
        val id = ids.incrementAndGet()
        mutableEntries.value = (mutableEntries.value + CommandEntry(
            id = id, command = command, state = CommandState.RUNNING
        )).takeLast(MAX_ENTRIES)
        return id
    }

    @Synchronized
    fun finish(id: Int, stdout: String, stderr: String, exitCode: Int, error: String? = null) {
        mutableEntries.value = mutableEntries.value.map { entry ->
            if (entry.id != id || entry.state != CommandState.RUNNING) entry
            else entry.copy(
                state = if (error != null || exitCode != 0) CommandState.FAILED else CommandState.FINISHED,
                stdout = stdout.takeLast(MAX_OUTPUT_CHARS),
                stderr = stderr.takeLast(MAX_OUTPUT_CHARS),
                exitCode = exitCode,
                error = error
            )
        }
    }

    @Synchronized
    fun fail(id: Int, message: String) {
        finish(id, "", "", -1, message)
    }

    @Synchronized
    fun clear() {
        mutableEntries.value = emptyList()
    }
}
