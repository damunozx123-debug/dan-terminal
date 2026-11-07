package com.dan.terminal.core.security

interface ShizukuAccess {
    val isBinderAlive: Boolean
    val isPermissionGranted: Boolean
    fun requestPermission()
    fun openShizukuApp()
    fun openShizukuDownload()
}
