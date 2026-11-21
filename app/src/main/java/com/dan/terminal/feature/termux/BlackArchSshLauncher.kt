package com.dan.terminal.feature.termux

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Explicit user-triggered handoff to Termux's existing SSH configuration.
 *
 * Android applications cannot read another app's private SSH key. This
 * launches SSH IN TERMUX, not inside our TerminalView. No keys are copied.
 */
internal object BlackArchSshLauncher {
    private const val TERMUX_PACKAGE = "com.termux"
    private const val RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND"

    // Kept as separate arguments; no shell interpolation or stored passwords.
    internal val arguments: Array<String>
        get() = arrayOf("-tt", "blackarch", "tmux", "new-session", "-A", "-s", "dan-phone")

    fun launch(context: Context): String {
        if (context.checkSelfPermission(RUN_COMMAND_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            return "Grant DAN Terminal the Termux Run commands permission in Android App settings, then retry."
        }

        val intent = Intent("com.termux.RUN_COMMAND").apply {
            component = ComponentName(TERMUX_PACKAGE, "com.termux.app.RunCommandService")
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/ssh")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arguments)
            putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
            putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
        }
        return try {
            val component = context.startService(intent)
            if (component == null) {
                "Termux did not accept the SSH request. Open Termux and check its installation."
            } else {
                "BlackArch SSH requested in Termux. Open Termux if it doesn't come to the foreground."
            }
        } catch (_: SecurityException) {
            "Termux blocked the request. Set allow-external-apps=true in Termux properties and grant Run commands permission."
        } catch (_: IllegalStateException) {
            "Android blocked starting Termux's command service. Bring Termux to the foreground, then try again."
        } catch (_: Exception) {
            "Couldn't launch SSH in Termux. Check Termux permissions and the BlackArch SSH profile."
        }
    }
}
