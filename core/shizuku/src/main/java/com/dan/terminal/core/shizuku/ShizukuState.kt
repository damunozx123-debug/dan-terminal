package com.dan.terminal.core.shizuku

data class ShizukuState(
    val binderAlive: Boolean = false,
    val permissionGranted: Boolean = false,
    val version: Int = -1,
    val preV11: Boolean = false,
    val permissionBlocked: Boolean = false,
    val permissionRequestPending: Boolean = false,
    val lastError: String? = null
) {
    val ready: Boolean get() = binderAlive && permissionGranted && !preV11
}
