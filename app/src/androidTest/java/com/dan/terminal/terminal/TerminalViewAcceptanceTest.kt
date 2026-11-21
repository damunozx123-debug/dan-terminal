package com.dan.terminal.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dan.terminal.MainActivity
import com.dan.terminal.feature.termux.PtyTerminalService
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class TerminalViewAcceptanceTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun completedSetupOpensTerminalImeInputAndActivityRecreationKeepShellState() {
        withTerminal { scenario ->
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            var original: TerminalSession? = null
            scenario.onActivity { activity ->
                original = checkNotNull(findTerminal(activity.window.decorView)).currentSession
            }
            val retained = checkNotNull(original)
            typeCommand(scenario, "cd /system; export M3_VIEW_MARK=retained; export M3_VIEW_PID=\$\$; printf '\\n%s=%s\\n' M3_VIEW READY")
            await(scenario) { it.currentSession.emulator.screen.transcriptText.contains("M3_VIEW=READY") }

            // Leaving this route disposes LocalShellScreen's service binding.
            // The started service, rather than a mounted view, must own the PTY.
            compose.onNodeWithText("Setup").performClick()
            compose.waitForIdle()
            scenario.onActivity { activity ->
                assertTrue("terminal view must be detached in Setup",
                    findTerminal(activity.window.decorView) == null)
            }
            instrumentation.runOnMainSync {
                assertTrue("started service must retain a live shell", retained.isRunning)
                // Send only after detaching. A marker assembled by printf cannot
                // be satisfied by the echoed command text.
                retained.write("test \"\$\$\" = \"\$M3_VIEW_PID\" && test \"\$PWD\" = /system && test \"\$M3_VIEW_MARK\" = retained && printf '\\n%s=%s\\n' M3_DETACHED retained\n")
            }
            awaitSessionText(retained, "M3_DETACHED=retained")
            compose.onNodeWithText("Open terminal console").performClick()
            await(scenario) { it.currentSession?.emulator != null }
            scenario.onActivity { activity ->
                val terminal = checkNotNull(findTerminal(activity.window.decorView))
                assertSame("navigation must reattach the same PTY", retained, terminal.currentSession)
                assertTrue(terminal.currentSession.emulator.screen.transcriptText.contains("M3_DETACHED=retained"))
            }

            scenario.recreate()
            await(scenario) { it.currentSession?.emulator != null }
            scenario.onActivity { activity ->
                assertSame("activity recreation must retain the PTY", retained,
                    checkNotNull(findTerminal(activity.window.decorView)).currentSession)
            }
            typeCommand(scenario, "test \"\$\$\" = \"\$M3_VIEW_PID\" && test \"\$PWD\" = /system && printf '\\n%s=%s\\n' M3_RECREATED \"\$M3_VIEW_MARK\"")
            await(scenario) { it.currentSession.emulator.screen.transcriptText.contains("M3_RECREATED=retained") }
        }
    }

    private fun awaitSessionText(session: TerminalSession, expected: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(12)
        var transcript = ""
        while (System.nanoTime() < deadline) {
            instrumentation.runOnMainSync {
                transcript = session.emulator.screen.transcriptText
            }
            if (transcript.contains(expected)) return
            Thread.sleep(50)
        }
        throw AssertionError("Detached PTY did not produce '$expected'. Transcript: $transcript")
    }

    @Test fun clipboardPastePreservesUnicodeWithoutSubmittingUntilEnter() = withTerminal { scenario ->
        typeCommand(scenario, "stty -echo; PS1=''; printf '\\n%s=%s\\n' M3_CLIP READY")
        await(scenario) { it.currentSession.emulator.screen.transcriptText.contains("M3_CLIP=READY") }
        requestPaste(scenario, "printf '\\n%s=%s\\n' M3_PASTED 'Māori ✓ 中文'")
        scenario.onActivity { activity ->
            val terminal = checkNotNull(findTerminal(activity.window.decorView))
            assertFalse(terminal.currentSession.emulator.screen.transcriptText.contains("M3_PASTED=Māori ✓ 中文"))
        }
        typeCommand(scenario, "")
        await(scenario) { it.currentSession.emulator.screen.transcriptText.contains("M3_PASTED=Māori ✓ 中文") }
    }

    @Test fun multilineClipboardPasteCanBeCancelledOrExplicitlyConfirmed() = withTerminal { scenario ->
        typeCommand(scenario, "stty -echo; PS1=''; printf '\\n%s=%s\\n' M3_MULTI READY")
        await(scenario) { it.currentSession.emulator.screen.transcriptText.contains("M3_MULTI=READY") }
        val text = "printf '\\n%s=%s\\n' M3_MULTI_ONE accepted\nprintf '\\n%s=%s\\n' M3_MULTI_TWO accepted\n"
        requestPaste(scenario, text)
        compose.onNodeWithText("Paste multiple lines?").assertIsDisplayed()
        compose.onNodeWithText("Cancel paste").performClick()
        compose.waitForIdle()
        scenario.onActivity { activity ->
            val terminal = checkNotNull(findTerminal(activity.window.decorView))
            assertFalse(terminal.currentSession.emulator.screen.transcriptText.contains("M3_MULTI_ONE=accepted"))
        }
        requestPaste(scenario, text)
        compose.onNodeWithText("Paste multiple lines?").assertIsDisplayed()
        compose.onNodeWithText("Paste text").performClick()
        await(scenario) { it.currentSession.emulator.screen.transcriptText.contains("M3_MULTI_TWO=accepted") }
        scenario.onActivity { activity ->
            val terminal = checkNotNull(findTerminal(activity.window.decorView))
            assertTrue(terminal.currentSession.emulator.screen.transcriptText.contains("M3_MULTI_ONE=accepted"))
        }
    }

    @Test fun defaultFontUsesScaledPixelsOnTheRealTerminalGrid() = withFontPreference(null) {
        withTerminal { scenario -> assertFontColumns(scenario, 14) }
    }

    @Test fun fontControlsResizeSameShellAndPersistAcrossRecreationAndNavigation() = withFontPreference(null) {
        withTerminal { scenario ->
            var original: TerminalSession? = null
            scenario.onActivity { activity -> original = findTerminal(activity.window.decorView)?.currentSession }
            typeCommand(scenario, "stty -echo; PS1=''; export FONT_MARK=retained; printf '\\n%s=%s\\n' FONT_READY YES")
            await(scenario) { it.currentSession.emulator.screen.transcriptText.contains("FONT_READY=YES") }
            compose.onNodeWithContentDescription("Increase terminal text size").performClick()
            compose.waitForIdle()
            assertFontColumns(scenario, 16)
            scenario.onActivity { activity -> assertSame(original, findTerminal(activity.window.decorView)?.currentSession) }
            scenario.recreate()
            await(scenario) { it.currentSession?.emulator != null }
            assertFontColumns(scenario, 16)
            compose.onNodeWithText("Setup").performClick()
            compose.onNodeWithText("Open terminal console").performClick()
            await(scenario) { it.currentSession?.emulator != null }
            assertFontColumns(scenario, 16)
            scenario.onActivity { activity -> assertSame(original, findTerminal(activity.window.decorView)?.currentSession) }
            typeCommand(scenario, "printf '\\n%s=%s\\n' FONT_AFTER \"\$FONT_MARK\"")
            await(scenario) { it.currentSession.emulator.screen.transcriptText.contains("FONT_AFTER=retained") }
            compose.onNodeWithContentDescription("Decrease terminal text size").performClick()
            compose.waitForIdle()
            assertFontColumns(scenario, 14)
        }
    }

    @Test fun fontControlsClampAtReadableMinimumAndMaximum() {
        withFontPreference(10) {
            withTerminal { scenario ->
                compose.onNodeWithContentDescription("Decrease terminal text size").assertIsNotEnabled()
                assertFontColumns(scenario, 10)
            }
        }
        withFontPreference(28) {
            withTerminal { scenario ->
                compose.onNodeWithContentDescription("Increase terminal text size").assertIsNotEnabled()
                assertFontColumns(scenario, 28)
            }
        }
    }

    private fun assertFontColumns(scenario: ActivityScenario<MainActivity>, sizeSp: Int) {
        compose.waitForIdle()
        scenario.onActivity { activity ->
            val terminal = checkNotNull(findTerminal(activity.window.decorView))
            val pixels = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, sizeSp.toFloat(), activity.resources.displayMetrics
            ).roundToInt()
            val paint = Paint().apply { typeface = Typeface.MONOSPACE; textSize = pixels.toFloat() }
            val expectedColumns = (terminal.width / paint.measureText("X")).toInt().coerceAtLeast(4)
            assertTrue("terminal must be laid out", terminal.width > 0)
            assertEquals("terminal grid must match the chosen scaled font", expectedColumns,
                terminal.currentSession.emulator.mColumns)
        }
    }

    private fun withFontPreference(sizeSp: Int?, block: () -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("dan_terminal_appearance", 0)
        val old = preferences.all["font_size_sp"]
        preferences.edit().apply {
            if (sizeSp == null) remove("font_size_sp") else putInt("font_size_sp", sizeSp)
        }.commit()
        try { block() } finally {
            preferences.edit().apply {
                if (old is Int) putInt("font_size_sp", old) else remove("font_size_sp")
            }.commit()
        }
    }

    private fun requestPaste(scenario: ActivityScenario<MainActivity>, text: String) {
        scenario.onActivity { activity ->
            val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Acceptance fixture", text))
            checkNotNull(findTerminal(activity.window.decorView)).currentSession.onPasteTextFromClipboard()
        }
    }

    private fun withTerminal(block: (ActivityScenario<MainActivity>) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = context.getSharedPreferences("dan_terminal_setup", 0)
        val oldSetup = preferences.getBoolean("setup_completed", false)
        preferences.edit().putBoolean("setup_completed", true).commit()
        context.stopService(Intent(context, PtyTerminalService::class.java))
        instrumentation.waitForIdleSync()
        Log.i("DAN-M3-TEST", "launching activity for terminal acceptance")
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                Log.i("DAN-M3-TEST", "activity launched; waiting for native terminal")
                try {
                    await(scenario) { it.currentSession?.emulator != null }
                    block(scenario)
                } finally {
                    // This timestamp distinguishes expected teardown detach from
                    // a premature detach while the test is still waiting.
                    Log.i("DAN-M3-TEST", "closing ActivityScenario")
                }
            }
        } finally {
            Log.i("DAN-M3-TEST", "stopping acceptance PTY service")
            context.stopService(Intent(context, PtyTerminalService::class.java))
            preferences.edit().putBoolean("setup_completed", oldSetup).commit()
        }
    }

    private fun typeCommand(scenario: ActivityScenario<MainActivity>, command: String) {
        scenario.onActivity { activity ->
            val view = checkNotNull(findTerminal(activity.window.decorView))
            view.requestFocus()
            // Exercise Android's IME bridge rather than writing directly to the session.
            view.onCreateInputConnection(EditorInfo()).commitText(command, 1)
            view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    private fun await(scenario: ActivityScenario<MainActivity>, predicate: (TerminalView) -> Boolean) {
        val started = System.nanoTime()
        val deadline = started + TimeUnit.SECONDS.toNanos(12)
        Log.i("DAN-M3-TEST", "native view wait started (12s bound)")
        while (System.nanoTime() < deadline) {
            var matched = false
            scenario.onActivity { activity ->
                findTerminal(activity.window.decorView)?.let { matched = predicate(it) }
            }
            if (matched) {
                Log.i("DAN-M3-TEST", "native view condition matched after " +
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) + "ms")
                return
            }
            Thread.sleep(50)
        }
        Log.i("DAN-M3-TEST", "native view wait timed out after " +
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) + "ms")
        // Capture what actually mounted, instead of hiding six distinct readiness
        // failures behind the same timeout message. No extra waiting or skipped tests.
        var diagnostic = "Activity unavailable"
        scenario.onActivity { activity ->
            val root = activity.window.decorView
            val terminal = findTerminal(root)
            val setup = activity.getSharedPreferences("dan_terminal_setup", 0)
                .getBoolean("setup_completed", false)
            val nodes = mutableListOf<String>()
            fun walk(node: View) {
                if (nodes.size >= 24) return
                nodes.add(node.javaClass.simpleName + "(" + node.width + "x" + node.height + ")")
                if (node is ViewGroup) {
                    for (index in 0 until node.childCount) walk(node.getChildAt(index))
                }
            }
            walk(root)
            diagnostic = listOf(
                "setupCompleted=" + setup,
                "terminalMounted=" + (terminal != null),
                "terminalShown=" + (terminal?.isShown ?: false),
                "terminalSize=" + (terminal?.width ?: -1) + "x" + (terminal?.height ?: -1),
                "sessionAttached=" + (terminal?.currentSession != null),
                "emulatorReady=" + (terminal?.currentSession?.emulator != null),
                "shellRunning=" + (terminal?.currentSession?.isRunning ?: false),
                "viewTree=" + nodes.joinToString(" > ")
            ).joinToString("; ")
        }
        val semantics = runCatching {
            val tree = compose.onRoot(useUnmergedTree = true).printToString(maxDepth = 8)
            // The first 2,400 characters hide the bottom-of-screen status text
            // ("Starting interactive shell" vs the real service/bind error).
            // Extract readable labels before keeping a bounded tree excerpt.
            val labels = tree.lineSequence()
                .filter { "Text = " in it || "ContentDescription = " in it }
                .joinToString(" | ")
                .take(6000)
            "visibleLabels=[" + labels + "]; treeTail=" + tree.takeLast(4000)
        }.getOrElse { "Unavailable: ${it.javaClass.simpleName}: ${it.message}" }
        throw AssertionError(
            "Terminal view did not reach expected state through Android input. " +
                diagnostic + "; composeSemantics=" + semantics
        )
    }

    private fun findTerminal(view: View): TerminalView? {
        if (view is TerminalView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            findTerminal(view.getChildAt(index))?.let { return it }
        }
        return null
    }
}
