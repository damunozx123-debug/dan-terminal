package com.dan.terminal

import com.dan.terminal.core.security.AccessCapability
import com.dan.terminal.core.security.AccessRecoveryManager
import com.dan.terminal.core.security.PermissionBroker
import com.dan.terminal.core.security.ShizukuAccess
import com.dan.terminal.core.security.TermuxAccess
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class AccessRecoveryManagerTest {

    private val shizuku = object : ShizukuAccess {
        override var isBinderAlive = false
        override var isPermissionGranted = false
        override fun refresh() = Unit
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

    @Test
    fun permissionFlowStaysOpenUntilBothServiceAndPermissionAreReady() {
        val manager = AccessRecoveryManager(mock(PermissionBroker::class.java), shizuku, termux)
        val coordinator = com.dan.terminal.core.security.AccessCoordinator(manager, shizuku)
        assertFalse(coordinator.require(AccessCapability.SHIZUKU_PERMISSION))
        val stopped = coordinator.activeRequirement.value!!
        assertEquals(AccessCapability.SHIZUKU_PERMISSION, stopped.capability)
        assertTrue(stopped.steps.any { it.action == com.dan.terminal.core.security.RecoveryAction.OPEN_DEVELOPER_OPTIONS })

        shizuku.isBinderAlive = true
        coordinator.verifyPending()
        val unauthorized = coordinator.activeRequirement.value!!
        assertTrue(unauthorized.steps.any { it.action == com.dan.terminal.core.security.RecoveryAction.REQUEST_SHIZUKU_PERMISSION })

        shizuku.isPermissionGranted = true
        coordinator.verifyPending()
        assertNull(coordinator.activeRequirement.value)
    }

    @Test
    fun permissionWithoutLiveServiceIsNeverGranted() {
        shizuku.isPermissionGranted = true
        val manager = AccessRecoveryManager(mock(PermissionBroker::class.java), shizuku, termux)
        assertFalse(manager.assess(AccessCapability.SHIZUKU_PERMISSION).granted)
    }
}
