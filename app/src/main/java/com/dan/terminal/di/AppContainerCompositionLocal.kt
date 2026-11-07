package com.dan.terminal.di

import androidx.compose.runtime.staticCompositionLocalOf
import com.dan.terminal.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}
