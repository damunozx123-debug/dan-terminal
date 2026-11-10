package com.dan.terminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.dan.terminal.di.LocalAppContainer
import com.dan.terminal.navigation.DanNavHost
import com.dan.terminal.ui.theme.DanTerminalTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as DanTerminalApp).container

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                DanTerminalTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                            DanNavHost()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val container = (application as DanTerminalApp).container
        container.shizukuManager.refresh()
        container.accessCoordinator.verifyPending()
    }
}
