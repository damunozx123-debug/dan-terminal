package com.dan.terminal.core.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccessCoordinator(
    private val recoveryManager: AccessRecoveryManager,
    private val shizukuAccess: ShizukuAccess
) {
    private val _activeRequirement = MutableStateFlow<AccessRequirement?>(null)
    val activeRequirement: StateFlow<AccessRequirement?> = _activeRequirement.asStateFlow()

    fun require(capability: AccessCapability): Boolean {
        val requirement = recoveryManager.assess(capability)
        if (!requirement.granted) {
            _activeRequirement.value = requirement
        }
        return requirement.granted
    }

    fun requestShizukuPermission() {
        shizukuAccess.requestPermission()
    }

    fun verifyPending() {
        val current = _activeRequirement.value ?: return
        val refreshed = recoveryManager.assess(current.capability)
        _activeRequirement.value = if (refreshed.granted) null else refreshed
    }

    fun dismiss() {
        _activeRequirement.value = null
    }
}
