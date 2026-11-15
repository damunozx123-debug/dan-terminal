package com.dan.terminal.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dan.terminal.MainActivity
import com.dan.terminal.feature.termux.PtyTerminalService
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TerminalViewAcceptanceTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun completedSetupOpensTerminalImeInputAndActivityRecreationKeepShellState() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = context.getSharedPreferences("dan_terminal_setup", 0)
        val oldSetup = preferences.getBoolean("setup_completed", false)
        preferences.edit().putBoolean("setup_completed", true).commit()
        context.stopService(Intent(context, PtyTerminalService::class.java))
        instrumentation.waitForIdleSync()
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                await(scenario) { it.currentSession?.emulator != null }
                var original: TerminalSession? = null
                scenario.onActivity { activity -> original = findTerminal(activity.window.decorView)?.currentSession }
                typeCommand(scenario, "cd \"\$HOME\"; export M3_VIEW_MARK=retained; printf '\\n%s=%s\\n' M3_VIEW READY")
                await(scenario) { it.currentSession.emulator.screen.transcriptText.contains("M3_VIEW=READY") }
                scenario.recreate()
                await(scenario) { it.currentSession?.emulator != null }
                scenario.onActivity { activity -> assertSame(original, findTerminal(activity.window.decorView)?.currentSession) }
                typeCommand(scenario, "printf '\\n%s=%s\\n' M3_RECREATED \"\$M3_VIEW_MARK\"")
                await(scenario) { it.currentSession.emulator.screen.transcriptText.contains("M3_RECREATED=retained") }
                assertTrue(preferences.getBoolean("setup_completed", false))
            }
        } finally {
            context.stopService(Intent(context, PtyTerminalService::class.java))
            preferences.edit().putBoolean("setup_completed", oldSetup).commit()
        }
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
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                await(scenario) { it.currentSession?.emulator != null }
                block(scenario)
            }
        } finally {
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
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(12)
        while (System.nanoTime() < deadline) {
            var matched = false
            scenario.onActivity { activity ->
                findTerminal(activity.window.decorView)?.let { matched = predicate(it) }
            }
            if (matched) return
            Thread.sleep(50)
        }
        throw AssertionError("Terminal view did not reach expected state through Android input")
    }

    private fun findTerminal(view: View): TerminalView? {
        if (view is TerminalView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            findTerminal(view.getChildAt(index))?.let { return it }
        }
        return null
    }
}
