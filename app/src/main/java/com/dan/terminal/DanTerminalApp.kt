package com.dan.terminal

import android.app.Application

class DanTerminalApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.shizukuManager.init()
    }
}
