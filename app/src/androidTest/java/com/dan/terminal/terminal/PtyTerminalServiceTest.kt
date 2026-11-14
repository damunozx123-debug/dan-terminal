package com.dan.terminal.terminal

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import com.dan.terminal.feature.termux.PtyTerminalService
import com.termux.terminal.TerminalSession
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

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
        session.updateSize(100, 40)
        send(session, "test -t 0 && printf '\\nM3_TTY=YES\\n' || printf '\\nM3_TTY=NO\\n'")
        awaitText(session, "M3_TTY=YES")

        send(session, "cd \"\$HOME\"; export M3_SESSION_MARK=dan; printf '\\nM3_PWD=%s\\nM3_MARK=%s\\n' \"\$PWD\" \"\$M3_SESSION_MARK\"")
        awaitText(session, "M3_MARK=dan")
        assertTrue(session.emulator.screen.transcriptText.contains("M3_PWD=$appHome"))

        send(session, "stty size")
        awaitText(session, "40 100")

        send(session, "sleep 30")
        awaitText(session, "sleep 30")
        Thread.sleep(300)
        sendBytes(session, byteArrayOf(0x03))
        send(session, "printf '\\nM3_AFTER_INTERRUPT=YES\\n'")
        awaitText(session, "M3_AFTER_INTERRUPT=YES")
        assertTrue("shell should remain available after Ctrl+C", session.isRunning)
    }

    private fun send(session: TerminalSession, command: String) {
        sendBytes(session, (command + "\n").toByteArray(Charsets.UTF_8))
    }

    private fun sendBytes(session: TerminalSession, bytes: ByteArray) {
        session.write(bytes, 0, bytes.size)
    }

    private fun awaitText(session: TerminalSession, expected: String) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(8)
        while (System.nanoTime() < deadline) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            if (session.emulator.screen.transcriptText.contains(expected)) return
            Thread.sleep(50)
        }
        throw AssertionError("Timed out waiting for '$expected'. Transcript: ${session.emulator.screen.transcriptText}")
    }
}
