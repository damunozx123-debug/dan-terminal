package com.dan.terminal.core.security

interface TermuxAccess {
    fun isInstalled(): Boolean
    fun hasRunCommandPermission(): Boolean
}
