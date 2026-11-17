package com.dan.terminal

import android.os.Bundle
import android.os.Build
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val incomingReportUri = MutableStateFlow<Uri?>(null)

    private fun receiveReportIntent(candidate: Intent?) {
        if (candidate?.action != Intent.ACTION_SEND ||
            candidate.type != "application/vnd.dan.diagnostic-report+json") return
        val uri: Uri? = if (Build.VERSION.SDK_INT >= 33) {
            candidate.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            candidate.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        }
        if (uri?.scheme == "content") incomingReportUri.value = uri
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveReportIntent(intent)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        receiveReportIntent(intent)
        enableEdgeToEdge()
        val container = (application as DanTerminalApp).container

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                DanTerminalTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                            DanNavHost(
                                incomingReportUri = incomingReportUri,
                                onReportConsumed = { incomingReportUri.value = null }
                            )
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
