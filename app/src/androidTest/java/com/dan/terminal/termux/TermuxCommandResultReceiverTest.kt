package com.dan.terminal.termux

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dan.terminal.feature.termux.CommandState
import com.dan.terminal.feature.termux.TermuxCommandResultReceiver
import com.dan.terminal.feature.termux.TermuxCommandResults
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TermuxCommandResultReceiverTest {
    @Before fun reset() = TermuxCommandResults.clear()
    @After fun cleanUp() = TermuxCommandResults.clear()

    @Test fun zeroExitCodeFinishesAndPreservesOutput() {
        val id = TermuxCommandResults.start("printf CALLBACK_OK")!!
        receive(id, Bundle().apply {
            putInt("err", Activity.RESULT_OK)
            putInt("exitCode", 0)
            putInt("exit_code", 7) // Not the PendingIntent bundle key; must be ignored.
            putString("stdout", "CALLBACK_OK\n")
            putString("stderr", "diagnostic")
        })
        val entry = TermuxCommandResults.entries.value.single()
        assertEquals(CommandState.FINISHED, entry.state)
        assertEquals(0, entry.exitCode)
        assertEquals("CALLBACK_OK\n", entry.stdout)
        assertEquals("diagnostic", entry.stderr)
        assertNull(entry.error)
    }

    @Test fun nonzeroExitCodeIsPreserved() {
        val id = TermuxCommandResults.start("exit 7")!!
        receive(id, Bundle().apply { putInt("exitCode", 7); putString("stderr", "failed") })
        val entry = TermuxCommandResults.entries.value.single()
        assertEquals(CommandState.FAILED, entry.state)
        assertEquals(7, entry.exitCode)
        assertEquals("failed", entry.stderr)
    }

    @Test fun executionErrorFailsEvenWhenExitCodeIsZero() {
        val id = TermuxCommandResults.start("command")!!
        receive(id, Bundle().apply {
            putInt("exitCode", 0)
            putInt("err", 2)
            putString("errmsg", "execution denied")
        })
        val entry = TermuxCommandResults.entries.value.single()
        assertEquals(CommandState.FAILED, entry.state)
        assertEquals(0, entry.exitCode)
        assertEquals("execution denied", entry.error)
    }

    @Test fun missingResultBundleFails() {
        val id = TermuxCommandResults.start("command")!!
        receive(id, null)
        val entry = TermuxCommandResults.entries.value.single()
        assertEquals(CommandState.FAILED, entry.state)
        assertTrue(entry.error.orEmpty().contains("no result bundle"))
    }

    @Test fun missingExitCodeNeverBecomesSuccess() {
        val id = TermuxCommandResults.start("command")!!
        receive(id, Bundle())
        val entry = TermuxCommandResults.entries.value.single()
        assertEquals(CommandState.FAILED, entry.state)
        assertEquals(-1, entry.exitCode)
    }

    @Test fun unknownRequestDoesNotChangeRunningCommand() {
        val id = TermuxCommandResults.start("command")!!
        receive(id + 999, Bundle().apply { putInt("exitCode", 0) })
        val entry = TermuxCommandResults.entries.value.single()
        assertEquals(id, entry.id)
        assertEquals(CommandState.RUNNING, entry.state)
    }

    private fun receive(id: Int, result: Bundle?) {
        val intent = Intent().putExtra(TermuxCommandResultReceiver.EXTRA_REQUEST_ID, id)
        if (result != null) intent.putExtra("result", result)
        TermuxCommandResultReceiver().onReceive(InstrumentationRegistry.getInstrumentation().targetContext, intent)
    }
}
