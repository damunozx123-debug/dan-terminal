package com.dan.terminal.terminal

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import com.dan.terminal.feature.termux.PtyTerminalService
import com.termux.terminal.TerminalSession
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class PtyTerminalServiceTest {
    @get:Rule
    val serviceRule = ServiceTestRule()

    @Test
    fun serviceOwnsInteractivePtyAcrossInputResizeAndInterrupt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val binder = serviceRule.bindService(Intent(context, PtyTerminalService::class.java))
            as PtyTerminalService.LocalBinder
        val session: TerminalSession = binder.session
        val appHome = context.filesDir.absolutePath

        assertTrue("shell should be running", session.isRunning)
        resizeOnMainThread(session, columns = 100, rows = 40)
        val dimensions = AtomicReference<Pair<Int, Int>>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            dimensions.set(session.emulator.mColumns to session.emulator.mRows)
        }
        assertTrue("resize should update emulator columns", dimensions.get().first == 100)
        assertTrue("resize should update emulator rows", dimensions.get().second == 40)
        send(session, "test -t 0 && printf '\\n%s=%s\\n' M3_TTY YES || printf '\\n%s=%s\\n' M3_TTY NO")
        awaitText(session, "M3_TTY=YES")

        send(session, "cd \"\$HOME\"; export M3_SESSION_MARK=dan; printf '\\n%s=%s\\n%s=%s\\n' M3_PWD \"\$PWD\" M3_MARK \"\$M3_SESSION_MARK\"")
        awaitText(session, "M3_MARK=dan")
        assertTrue(transcriptTextOnMainThread(session).contains("M3_PWD=$appHome"))

        send(session, "stty size")
        awaitText(session, "40 100")

        send(session, "printf '\\n%s=%s\\n' M3_SLEEP READY; sleep 30")
        awaitText(session, "M3_SLEEP=READY")
        sendBytes(session, byteArrayOf(0x03))
        send(session, "printf '\\n%s=%s\\n' M3_AFTER_INTERRUPT YES")
        awaitText(session, "M3_AFTER_INTERRUPT=YES")
        assertTrue("shell should remain available after Ctrl+C", session.isRunning)
    }

    @Test
    fun exactly80ColumnsThenWrapDoesNotAddressColumn80() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val binder = serviceRule.bindService(Intent(context, PtyTerminalService::class.java))
            as PtyTerminalService.LocalBinder
        val session = binder.session
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        resizeOnMainThread(session, columns = 80, rows = 24)
        val dimensions = AtomicReference<Pair<Int, Int>>()
        instrumentation.runOnMainSync {
            dimensions.set(session.emulator.mColumns to session.emulator.mRows)
        }
        assertTrue("terminal should use exactly 80 columns", dimensions.get().first == 80)
        assertTrue("terminal should use 24 rows", dimensions.get().second == 24)

        // Disable echo/prompts, then clear/home the cursor: exactly 80 cells (0..79),
        // followed by X at row 1, column 0. Assertions inspect actual output, not echo.
        send(session, "stty -echo; PS1=''; printf '\\n%s=%s\\n' M3_EDGE READY")
        awaitText(session, "M3_EDGE=READY")
        send(session, "printf '\\033[2J\\033[H%080dX' 0")
        awaitText(session, "0".repeat(80) + "X")
        instrumentation.runOnMainSync {
            val screen = session.emulator.screen
            assertEquals("0".repeat(80), screen.getSelectedText(0, 0, 79, 0))
            assertEquals("X", screen.getSelectedText(0, 1, 0, 1))
            assertTrue(session.emulator.cursorCol in 0..79)
        }

        // Exercise reflow while the output queue is active. Only the main thread
        // may mutate/read emulator state, just as TerminalView does in production.
        // Confirm the shell accepted the complete command before resizing.
        // Otherwise resize can race command input, masking the output-reflow check.
        // A short delay per output line keeps resize and rendering genuinely concurrent.
        send(session, "printf '\\n%s=%s\\n' M3_REFLOW START; i=0; while [ \$i -lt 100 ]; do printf '%080dX\\n' 0; sleep 0.02; i=\$((i+1)); done; printf '\\n%s=%s\\n' M3_REFLOW OK")
        awaitText(session, "M3_REFLOW=START")
        repeat(10) {
            resizeOnMainThread(session, columns = 100, rows = 40)
            resizeOnMainThread(session, columns = 80, rows = 24)
        }
        // 100 paced lines plus 20 synchronous resizes can exceed 8s on CI emulators.
        // Keep the output assertion, but give only this stress case a bounded 30s budget.
        awaitText(session, "M3_REFLOW=OK", timeoutSeconds = 30)
        send(session, "stty size")
        awaitText(session, "24 80")
    }

    private fun send(session: TerminalSession, command: String) {
        sendBytes(session, (command + "\n").toByteArray(Charsets.UTF_8))
    }

    @Test fun exitedShellCanBeRestartedAsAFreshSession() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val binder = serviceRule.bindService(Intent(context, PtyTerminalService::class.java)) as PtyTerminalService.LocalBinder
        val old = binder.session
        send(old, "export M3_OLD_STATE=old; exit 7")
        awaitText(old, "[Process completed (code 7)")
        assertTrue(!old.isRunning)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { binder.restartExitedSession() }
        val fresh = binder.session
        assertTrue(fresh !== old && fresh.isRunning)
        send(fresh, "printf '\\n%s=%s\\n' M3_NEW_STATE \"\${M3_OLD_STATE:-fresh}\"")
        awaitText(fresh, "M3_NEW_STATE=fresh")
    }

    private fun sendBytes(session: TerminalSession, bytes: ByteArray) {
        session.write(bytes, 0, bytes.size)
    }

    private fun awaitText(session: TerminalSession, expected: String, timeoutSeconds: Long = 8) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
        while (System.nanoTime() < deadline) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            if (transcriptTextOnMainThread(session).contains(expected)) return
            Thread.sleep(50)
        }
        throw AssertionError("Timed out waiting for '$expected'. Transcript: ${transcriptTextOnMainThread(session)}")
    }

    private fun resizeOnMainThread(session: TerminalSession, columns: Int, rows: Int) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            session.updateSize(columns, rows)
        }
    }

    private fun transcriptTextOnMainThread(session: TerminalSession): String {
        val transcript = AtomicReference<String>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            transcript.set(session.emulator.screen.transcriptText)
        }
        return transcript.get()
    }
}
