package com.dan.terminal.feature.termux

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Not exported. Only a one-shot explicit PendingIntent supplied by this app is
 * passed to Termux. Do not log stdout/stderr: results may contain secrets.
 */
class TermuxCommandResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_REQUEST_ID, -1)
        if (id < 0) return

        val result = intent.getBundleExtra("result")
        if (result == null) {
            TermuxCommandResults.fail(id, "Termux returned no result bundle.")
            return
        }

        val internalError = result.getInt("err", Activity.RESULT_OK)
        val message = result.getString("errmsg")
        val executionError = if (internalError != Activity.RESULT_OK) {
            message ?: "Termux execution error ($internalError)"
        } else null

        TermuxCommandResults.finish(
            id = id,
            stdout = result.getString("stdout").orEmpty(),
            stderr = result.getString("stderr").orEmpty(),
            exitCode = result.getInt("exit_code", -1),
            error = executionError
        )
    }

    companion object {
        const val EXTRA_REQUEST_ID = "com.dan.terminal.extra.COMMAND_REQUEST_ID"
    }
}
