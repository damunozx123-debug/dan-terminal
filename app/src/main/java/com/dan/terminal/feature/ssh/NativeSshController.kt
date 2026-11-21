package com.dan.terminal.feature.ssh

import android.os.Handler
import android.os.Looper
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalOutput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.PTYMode
import net.schmizz.sshj.connection.channel.direct.Session
import java.nio.charset.StandardCharsets

/**
 * App-owned, interactive SSH transport. No Termux dependency or copied key.
 * Host key must be pinned BEFORE any credential is sent.
 */
data class NativeSshTarget(
    val host: String,
    val port: Int,
    val username: String,
    val hostFingerprint: String
) {
    fun validate(): String? {
        if (host.isBlank() || host.any { it.isWhitespace() }) return "Enter a valid SSH host."
        if (port !in 1..65535) return "SSH port must be 1–65535."
        if (username.isBlank() || username.any { it.isWhitespace() }) return "Enter an SSH username."
        if (!SHA256_FINGERPRINT.matches(hostFingerprint.trim())) {
            return "Enter the verified SHA256 host-key fingerprint from BlackArch."
        }
        return null
    }

    companion object {
        val SHA256_FINGERPRINT = Regex("^SHA256:[A-Za-z0-9+/]{43}=?$")
    }
}

internal class NativeSshController {
    private val main = Handler(Looper.getMainLooper())
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeMutex = Mutex()
    private var connection: Job? = null

    @Volatile private var client: SSHClient? = null
    @Volatile private var shell: Session.Shell? = null
    @Volatile private var stopped = false

    val status: StateFlow<String> get() = _status
    private val _status = MutableStateFlow("Disconnected")
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> get() = _connected

    // Assigned by the active Android native terminal View.
    var redraw: (() -> Unit)? = null
    var title: String = "BlackArch"

    private val terminalOutput = object : TerminalOutput() {
        override fun write(data: ByteArray, offset: Int, count: Int) {
            if (count > 0) sendBytes(data.copyOfRange(offset, offset + count))
        }

        override fun titleChanged(oldTitle: String?, newTitle: String?) {
            title = newTitle.orEmpty().take(80)
        }

        override fun onCopyTextToClipboard(text: String?) {
            // A remote host cannot silently write into the Android clipboard.
        }

        override fun onPasteTextFromClipboard() {
            // Explicit paste UI is a future feature; ignore remote clipboard requests.
        }

        override fun onBell() = Unit
        override fun onColorsChanged() { redraw?.invoke() }
    }

    /** The emulator is mutated only on the Android main/UI thread. */
    val emulator: TerminalEmulator =
        TerminalEmulator(terminalOutput, 80, 24, 8, 16, 2000, null)

    fun connect(target: NativeSshTarget, password: String) {
        val problem = target.validate()
        if (problem != null) {
            _status.value = problem
            return
        }
        if (password.isEmpty()) {
            _status.value = "Enter the BlackArch account password."
            return
        }
        if (connection?.isActive == true || stopped) return

        _status.value = "Connecting and verifying host key…"
        connection = io.launch {
            val ssh = SSHClient()
            client = ssh
            try {
                ssh.connectTimeout = 10_000
                // Do not set a socket read timeout: an idle interactive shell is normal.
                // Never use PromiscuousVerifier or accept unknown fingerprints.
                // SSHJ verifies the pinned SHA256 fingerprint during key exchange.
                ssh.addHostKeyVerifier(target.hostFingerprint.trim())
                ssh.connect(target.host.trim(), target.port)
                _status.value = "Authenticating…"
                ssh.authPassword(target.username.trim(), password)

                val session = ssh.startSession()
                session.allocatePTY(
                    "xterm-256color",
                    emulator.mColumns.coerceIn(4, 240),
                    emulator.mRows.coerceIn(4, 120),
                    0,
                    0,
                    emptyMap<PTYMode, Int>()
                )
                val remote = session.startShell()
                shell = remote
                _connected.value = true
                _status.value = "Connected (verified host key)"

                // Attach the known persistent tmux session; this is a fixed
                // command, never a user-interpolated shell expression.
                writeDirect("tmux new-session -A -s dan-phone\n".toByteArray(StandardCharsets.UTF_8))

                val buffer = ByteArray(8192)
                while (isActive && !stopped) {
                    val count = remote.inputStream.read(buffer)
                    if (count < 0) break
                    if (count > 0) {
                        val received = buffer.copyOf(count)
                        main.post {
                            if (!stopped) {
                                emulator.append(received, received.size)
                                redraw?.invoke()
                            }
                        }
                    }
                }
            } catch (_: CancellationException) {
                // Explicit disconnect, not an authentication failure.
            } catch (error: Exception) {
                _status.value = "SSH connection failed: " +
                    (error.message ?: error.javaClass.simpleName).take(180)
            } finally {
                _connected.value = false
                shell = null
                runCatching { ssh.disconnect() }
                runCatching { ssh.close() }
                if (!stopped && !_status.value.startsWith("SSH connection failed")) {
                    _status.value = "Disconnected"
                }
            }
        }
    }

    private suspend fun writeDirect(bytes: ByteArray) {
        writeMutex.withLock {
            shell?.outputStream?.apply {
                write(bytes)
                flush()
            }
        }
    }

    fun sendBytes(bytes: ByteArray) {
        if (!_connected.value || stopped) return
        io.launch {
            try {
                writeDirect(bytes)
            } catch (error: Exception) {
                _status.value = "Unable to write SSH input: " +
                    (error.message ?: "connection closed").take(120)
            }
        }
    }

    fun sendText(text: String) = sendBytes(text.toByteArray(StandardCharsets.UTF_8))

    /** Called on UI thread after the native view's size changes. */
    fun resize(columns: Int, rows: Int, cellWidth: Int, cellHeight: Int) {
        val cols = columns.coerceIn(4, 240)
        val lines = rows.coerceIn(4, 120)
        emulator.resize(cols, lines, cellWidth.coerceAtLeast(1), cellHeight.coerceAtLeast(1))
        redraw?.invoke()
        io.launch {
            runCatching { shell?.changeWindowDimensions(cols, lines, 0, 0) }
        }
    }

    fun disconnect() {
        stopped = true
        _connected.value = false
        _status.value = "Disconnected"
        connection?.cancel()
        io.launch {
            runCatching { shell?.close() }
            runCatching { client?.disconnect() }
            runCatching { client?.close() }
            io.cancel()
        }
    }
}
