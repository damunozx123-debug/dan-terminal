package com.dan.terminal.core.shizuku

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.dan.terminal.core.security.ShizukuAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Application-scoped; lifecycle and UI calls use the main thread. */
class ShizukuManager internal constructor(
    private val context: Context,
    private val api: ShizukuApi
) : ShizukuAccess {
    constructor(context: Context) : this(context, OfficialShizukuApi)

    private val _state = MutableStateFlow(ShizukuState())
    val state: StateFlow<ShizukuState> = _state.asStateFlow()
    private var unsubscribe: (() -> Unit)? = null
    private var active = false

    fun init() {
        if (active) return
        active = true
        try {
            unsubscribe = api.subscribe(
                onReceived = {
                    if (active) {
                        _state.value = ShizukuState()
                        refresh()
                    }
                },
                onDead = {
                    if (active) _state.value = ShizukuState(lastError = SERVICE_STOPPED)
                },
                onResult = { code, granted ->
                    if (active && code == REQUEST_CODE) {
                        _state.value = _state.value.copy(permissionRequestPending = false)
                        refresh()
                        if (!granted && !_state.value.permissionGranted &&
                            _state.value.binderAlive && !_state.value.preV11) {
                            _state.value = _state.value.copy(
                                permissionGranted = false,
                                lastError = "Shizuku permission was denied. Open Shizuku to review DAN Terminal's access."
                            )
                        }
                    }
                }
            )
            refresh()
        } catch (_: Exception) {
            active = false
            _state.value = ShizukuState(lastError = "Could not observe Shizuku. Reopen DAN Terminal and try again.")
        }
    }

    fun destroy() {
        active = false
        val remove = unsubscribe
        unsubscribe = null
        _state.value = ShizukuState()
        try {
            remove?.invoke()
        } catch (_: Exception) {
            _state.value = ShizukuState(lastError = "Could not remove all Shizuku listeners. Restart DAN Terminal.")
        }
    }

    override fun refresh() {
        try {
            if (!api.pingBinder()) {
                _state.value = ShizukuState(lastError = SERVICE_STOPPED)
                return
            }
            val legacy = api.isPreV11()
            val version = api.version()
            val granted = !legacy && api.permissionGranted()
            val blocked = !legacy && !granted && api.shouldShowRationale()
            _state.value = ShizukuState(
                binderAlive = true,
                permissionGranted = granted,
                version = version,
                preV11 = legacy,
                permissionBlocked = blocked,
                permissionRequestPending = !legacy && !granted && !blocked &&
                    _state.value.permissionRequestPending,
                lastError = when {
                    legacy -> "This Shizuku service is too old. Update Shizuku and restart its service."
                    granted -> null
                    blocked -> "Permission requests are blocked. Open Shizuku and review DAN Terminal's access."
                    else -> _state.value.lastError.takeUnless { it == SERVICE_STOPPED }
                }
            )
        } catch (_: Exception) {
            // An IPC failure must never leave a previously granted state visible.
            _state.value = ShizukuState(lastError = "Cannot verify Shizuku access. Open Shizuku, then recheck.")
        }
    }

    /** Explicit recovery when the external permission dialog never sends a result. */
    fun recheckPermission() {
        _state.value = _state.value.copy(permissionRequestPending = false)
        refresh()
    }

    override val isBinderAlive: Boolean get() = _state.value.binderAlive
    override val isPermissionGranted: Boolean get() = _state.value.ready

    override fun requestPermission() {
        if (!active) init()
        if (!active) return
        refresh()
        val current = _state.value
        if (!current.binderAlive || current.preV11 || current.permissionGranted ||
            current.permissionBlocked || current.permissionRequestPending) return
        _state.value = current.copy(permissionRequestPending = true, lastError = null)
        try {
            api.requestPermission(REQUEST_CODE)
        } catch (_: Exception) {
            _state.value = ShizukuState(lastError = "Shizuku permission request failed. Open Shizuku, then recheck.")
        }
    }

    override fun openShizukuApp() {
        try {
            val launch = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
            if (launch == null) {
                openShizukuDownload()
            } else {
                context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        } catch (_: Exception) {
            _state.value = _state.value.copy(lastError = "Could not open Shizuku. Open it from your launcher.")
        }
    }

    override fun openShizukuDownload() {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            _state.value = _state.value.copy(lastError = "Could not open the browser. Visit https://shizuku.rikka.app/download/.")
        }
    }

    companion object {
        private const val REQUEST_CODE = 4001
        private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private const val SERVICE_STOPPED = "Shizuku is not running. Open Shizuku, pair or start the service, then return."
    }
}
