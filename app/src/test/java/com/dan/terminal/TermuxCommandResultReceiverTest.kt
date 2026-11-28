package com.dan.terminal

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.dan.terminal.feature.termux.CommandState
import com.dan.terminal.feature.termux.TermuxCommandResultReceiver
import com.dan.terminal.feature.termux.TermuxCommandResults
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.CALLS_REAL_METHODS
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class TermuxCommandResultReceiverTest {
    @Before fun prepare() = TermuxCommandResults.clear()
    @After fun cleanup() = TermuxCommandResults.clear()

    @Test fun successfulTermuxCallbackPreservesCamelCaseExitCodeAndOutput() {
        deliver(exitCode = 0)

        val entry = TermuxCommandResults.entries.value.single()
        assertEquals(CommandState.FINISHED, entry.state)
        assertEquals(0, entry.exitCode)
        assertEquals("CALLBACK_OK\n", entry.stdout)
        assertEquals("diagnostic\n", entry.stderr)
        assertNull(entry.error)
    }

    @Test fun nonzeroTermuxCallbackPreservesTheActualExitCode() {
        deliver(exitCode = 7)

        val entry = TermuxCommandResults.entries.value.single()
        assertEquals(CommandState.FAILED, entry.state)
        assertEquals(7, entry.exitCode)
    }

    @Test fun executionErrorCannotBeReportedAsSuccess() {
        deliver(exitCode = 0, internalError = 1, errorMessage = "Executable is missing")

        val entry = TermuxCommandResults.entries.value.single()
        assertEquals(CommandState.FAILED, entry.state)
        assertEquals("Executable is missing", entry.error)
    }

    @Test fun missingExitCodeRemainsAFailure() {
        deliver(exitCode = null)

        val entry = TermuxCommandResults.entries.value.single()
        assertEquals(CommandState.FAILED, entry.state)
        assertEquals(-1, entry.exitCode)
    }

    @Test fun missingResultBundleReportsAnActionableFailure() {
        val id = requireNotNull(TermuxCommandResults.start("test"))
        receive(id, null)

        val entry = TermuxCommandResults.entries.value.single()
        assertEquals(CommandState.FAILED, entry.state)
        assertEquals("Termux returned no result bundle.", entry.error)
    }

    private fun deliver(exitCode: Int?, internalError: Int = Activity.RESULT_OK,
                        errorMessage: String? = null) {
        val id = requireNotNull(TermuxCommandResults.start("test"))
        // Android Bundle accessors are platform boundaries. Keep the receiver and
        // result store real, defaulting absent keys exactly as Bundle does.
        val bundle = mock(Bundle::class.java)
        val integers = mutableMapOf("err" to internalError)
        if (exitCode != null) integers["exitCode"] = exitCode
        `when`(bundle.getInt(anyString(), anyInt())).thenAnswer { call ->
            integers[call.getArgument<String>(0)] ?: call.getArgument<Int>(1)
        }
        `when`(bundle.getString("stdout")).thenReturn("CALLBACK_OK\n")
        `when`(bundle.getString("stderr")).thenReturn("diagnostic\n")
        `when`(bundle.getString("errmsg")).thenReturn(errorMessage)
        receive(id, bundle)
    }

    private fun receive(id: Int, bundle: Bundle?) {
        val intent = mock(Intent::class.java)
        `when`(intent.getIntExtra(TermuxCommandResultReceiver.EXTRA_REQUEST_ID, -1)).thenReturn(id)
        `when`(intent.getBundleExtra("result")).thenReturn(bundle)
        // Skip only BroadcastReceiver's Android-only constructor; execute the
        // receiver's actual onReceive implementation against the real store.
        mock(TermuxCommandResultReceiver::class.java, CALLS_REAL_METHODS)
            .onReceive(mock(Context::class.java), intent)
    }
}
