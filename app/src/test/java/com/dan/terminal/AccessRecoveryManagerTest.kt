package com.dan.terminal

import com.dan.terminal.core.security.AccessCapability
import com.dan.terminal.core.security.AccessRecoveryManager
import com.dan.terminal.core.security.PermissionBroker
import com.dan.terminal.core.security.ShizukuAccess
import com.dan.terminal.core.security.TermuxAccess
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class AccessRecoveryManagerTest {

    private val shizuku = object : ShizukuAccess {
        override val isBinderAlive = false
        override val isPermissionGranted = false
        override fun requestPermission() = Unit
        override fun openShizukuApp() = Unit
        override fun openShizukuDownload() = Unit
    }

    private val termux = object : TermuxAccess {
        override fun isInstalled() = false
        override fun hasRunCommandPermission() = false
    }

    @Test
    fun missingShizukuServiceProducesRecoverySteps() {
        val manager = AccessRecoveryManager(
            permissionBroker = mock(PermissionBroker::class.java),
            shizukuAccess = shizuku,
            termuxAccess = termux
        )
        val requirement = manager.assess(AccessCapability.SHIZUKU_SERVICE)
        assertFalse(requirement.granted)
        assertTrue(requirement.steps.isNotEmpty())
        assertTrue(requirement.steps.any { it.title.contains("Developer", ignoreCase = true) })
    }

    @Test
    fun missingTermuxProducesInstallStep() {
        val manager = AccessRecoveryManager(
            permissionBroker = mock(PermissionBroker::class.java),
            shizukuAccess = shizuku,
            termuxAccess = termux
        )
        val requirement = manager.assess(AccessCapability.TERMUX_APP)
        assertFalse(requirement.granted)
        assertTrue(requirement.steps.any { it.title.contains("Install", ignoreCase = true) })
    }
}
