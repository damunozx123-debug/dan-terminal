package com.dan.terminal.core.shizuku

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/** Injectable boundary: permission-flow tests do not start an Android binder. */
internal interface ShizukuApi {
    fun pingBinder(): Boolean
    fun isPreV11(): Boolean
    fun version(): Int
    fun permissionGranted(): Boolean
    fun shouldShowRationale(): Boolean
    fun requestPermission(code: Int)
    fun subscribe(
        onReceived: () -> Unit,
        onDead: () -> Unit,
        onResult: (Int, Boolean) -> Unit
    ): () -> Unit
}

internal object OfficialShizukuApi : ShizukuApi {
    override fun pingBinder() = Shizuku.pingBinder()
    override fun isPreV11() = Shizuku.isPreV11()
    override fun version() = Shizuku.getVersion()
    override fun permissionGranted() = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    override fun shouldShowRationale() = Shizuku.shouldShowRequestPermissionRationale()
    override fun requestPermission(code: Int) = Shizuku.requestPermission(code)

    override fun subscribe(
        onReceived: () -> Unit,
        onDead: () -> Unit,
        onResult: (Int, Boolean) -> Unit
    ): () -> Unit {
        val received = Shizuku.OnBinderReceivedListener { onReceived() }
        val dead = Shizuku.OnBinderDeadListener { onDead() }
        val result = Shizuku.OnRequestPermissionResultListener { code, grant ->
            onResult(code, grant == PackageManager.PERMISSION_GRANTED)
        }
        val unsubscribe: () -> Unit = {
            val removals = listOf(
                runCatching { Shizuku.removeBinderReceivedListener(received) },
                runCatching { Shizuku.removeBinderDeadListener(dead) },
                runCatching { Shizuku.removeRequestPermissionResultListener(result) }
            )
            removals.firstOrNull { it.isFailure }?.getOrThrow()
            Unit
        }
        try {
            Shizuku.addBinderDeadListener(dead)
            Shizuku.addRequestPermissionResultListener(result)
            Shizuku.addBinderReceivedListenerSticky(received)
        } catch (failure: Exception) {
            runCatching { unsubscribe() }.exceptionOrNull()?.let(failure::addSuppressed)
            throw failure
        }
        return unsubscribe
    }
}
