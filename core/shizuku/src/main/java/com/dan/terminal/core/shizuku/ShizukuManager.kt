package com.dan.terminal.core.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.dan.terminal.core.security.ShizukuAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

class ShizukuManager(private val context: Context) : ShizukuAccess {

    private val _state = MutableStateFlow(ShizukuState())
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener { refresh() }
    private val binderDeadListener = Shizuku.OnBinderDeadListener { refresh() }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, _ ->
            if (requestCode == REQUEST_CODE) refresh()
        }

    fun init() {
        runCatching {
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        }
        refresh()
    }

    fun destroy() {
        runCatching {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        }
    }

    fun refresh() {
        val alive = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        val permission = if (alive) {
            runCatching {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }.getOrDefault(false)
        } else {
            false
        }
        val version = if (alive) runCatching { Shizuku.getVersion() }.getOrDefault(-1) else -1
        _state.value = ShizukuState(
            binderAlive = alive,
            permissionGranted = permission,
            version = version,
            preV11 = runCatching { Shizuku.isPreV11() }.getOrDefault(false)
        )
    }

    override val isBinderAlive: Boolean
        get() = _state.value.binderAlive

    override val isPermissionGranted: Boolean
        get() = _state.value.permissionGranted

    override fun requestPermission() {
        if (!isBinderAlive) return
        if (isPermissionGranted) {
            refresh()
            return
        }
        runCatching {
            Shizuku.requestPermission(REQUEST_CODE)
        }.onFailure {
            _state.value = _state.value.copy(lastError = it.message)
        }
    }

    override fun openShizukuApp() {
        val launch = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launch)
        } else {
            openShizukuDownload()
        }
    }

    override fun openShizukuDownload() {
        runCatching {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://shizuku.rikka.app/download/")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun isShizukuInstalled(): Boolean =
        context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE) != null

    companion object {
        private const val REQUEST_CODE = 4001
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }
}
