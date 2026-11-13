package com.dan.terminal.feature.termux

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.BufferedWriter
import java.io.OutputStreamWriter

/**
 * First persistent shell transport. A pipe is NOT a PTY: full-screen terminal
 * programs and job control require the subsequent native PTY backend.
 * Runs as the application's Android UID, never as Shizuku/root.
 */
class LocalShellSession {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _output = MutableStateFlow("Starting local shell…\n")
    val output = _output.asStateFlow()
    private val _running = MutableStateFlow(false)
    val running = _running.asStateFlow()
    private var process: Process? = null
    private var writer: BufferedWriter? = null

    @Synchronized
    fun start() {
        if (process?.isAlive == true) return
        try {
            val p = ProcessBuilder("/system/bin/sh")
                .redirectErrorStream(true)
                .start()
            process = p
            writer = BufferedWriter(OutputStreamWriter(p.outputStream))
            _running.value = true
            append("Local Android shell started (app sandbox; no PTY).\n$ ")
            scope.launch {
                try {
                    p.inputStream.bufferedReader().use { reader ->
                        val buffer = CharArray(2048)
                        while (true) {
                            val count = reader.read(buffer)
                            if (count < 0) break
                            append(String(buffer, 0, count))
                        }
                    }
                } catch (e: Exception) {
                    append("\nRead error: ${e.message}\n")
                } finally {
                    _running.value = false
                    append("\n[Shell exited]\n")
                }
            }
        } catch (e: Exception) {
            append("Unable to start shell: ${e.message}\n")
        }
    }

    @Synchronized
    fun send(command: String) {
        if (process?.isAlive != true) {
            append("Shell is not running.\n")
            return
        }
        try {
            append(command + "\n")
            writer?.apply { write(command); newLine(); flush() }
        } catch (e: Exception) {
            append("Write error: ${e.message}\n")
        }
    }

    @Synchronized
    fun stop() {
        writer?.close()
        writer = null
        process?.destroy()
        process = null
        _running.value = false
    }

    @Synchronized
    private fun append(text: String) {
        _output.value = (_output.value + text).takeLast(65536)
    }

    fun dispose() {
        stop()
        scope.cancel()
    }
}
