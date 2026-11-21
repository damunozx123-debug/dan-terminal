package com.dan.terminal.feature.termux

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Base64
import com.dan.terminal.core.security.TermuxAccess

class TermuxBridge(private val context: Context) : TermuxAccess {

    override fun isInstalled(): Boolean = runCatching {
        context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
        true
    }.getOrDefault(false)

    override fun hasRunCommandPermission(): Boolean =
        context.checkSelfPermission(RUN_COMMAND_PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun runCommand(
        command: String,
        background: Boolean = true,
        sessionAction: String = "0"
    ): Boolean {
        if (!isInstalled() || !hasRunCommandPermission()) return false

        val request = TermuxRunCommandContract.shell(command)
        val intent = Intent().apply {
            action = ACTION_RUN_COMMAND
            setClassName(TERMUX_PACKAGE, TERMUX_RUN_COMMAND_SERVICE)
            putExtra(EXTRA_COMMAND_PATH, request.executablePath)
            putExtra(EXTRA_ARGUMENTS, request.arguments.toTypedArray())
            putExtra(EXTRA_BACKGROUND, background)
            putExtra(EXTRA_SESSION_ACTION, sessionAction)
        }
        return runCatching {
            context.startService(intent) != null
        }.getOrDefault(false)
    }

    /**
     * Dispatch a one-shot Bash command and receive its output via Termux's
     * RUN_COMMAND PendingIntent API. Each command is a separate shell process.
     * The callback receiver is explicit and not exported.
     */
    fun runCommandWithResult(command: String): Boolean {
        if (!isInstalled() || !hasRunCommandPermission()) return false
        val id = TermuxCommandResults.start(command) ?: return false
        return runCatching {
            val callback = Intent(context, TermuxCommandResultReceiver::class.java).apply {
                action = "com.dan.terminal.action.COMMAND_RESULT"
                putExtra(TermuxCommandResultReceiver.EXTRA_REQUEST_ID, id)
            }
            val pendingResult = PendingIntent.getBroadcast(
                context,
                id,
                callback,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE
            )
            val request = TermuxRunCommandContract.shell(command)
            val intent = Intent().apply {
                action = ACTION_RUN_COMMAND
                setClassName(TERMUX_PACKAGE, TERMUX_RUN_COMMAND_SERVICE)
                putExtra(EXTRA_COMMAND_PATH, request.executablePath)
                putExtra(EXTRA_ARGUMENTS, request.arguments.toTypedArray())
                putExtra(EXTRA_BACKGROUND, true)
                putExtra(EXTRA_PENDING_INTENT, pendingResult)
            }
            checkNotNull(context.startService(intent)) { "Termux service was not started" }
            true
        }.getOrElse { error ->
            TermuxCommandResults.fail(id, error.message ?: "Unable to start Termux")
            false
        }
    }

    fun runBootstrap(script: String): Boolean {
        val encoded = Base64.encodeToString(script.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val command = "echo '$encoded' | base64 -d > \$HOME/.dan_bootstrap.sh && " +
            "bash \$HOME/.dan_bootstrap.sh"
        // Keep installation progress and package-manager prompts visible in Termux.
        return runCommand(command, background = false)
    }

    fun loadBootstrapScript(): String =
        context.assets.open("bootstrap/dan_bootstrap.sh")
            .bufferedReader()
            .use { it.readText() }

    companion object {
        private const val TERMUX_PACKAGE = "com.termux"
        private const val TERMUX_RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
        private const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
        private const val RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND"
        private const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
        private const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
        private const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
        private const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
        private const val EXTRA_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"
    }
}
