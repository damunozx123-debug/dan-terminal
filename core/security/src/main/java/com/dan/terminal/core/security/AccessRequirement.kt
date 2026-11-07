package com.dan.terminal.core.security

data class RecoveryStep(
    val title: String,
    val description: String,
    val action: RecoveryAction? = null
)

data class AccessRequirement(
    val capability: AccessCapability,
    val granted: Boolean,
    val reason: String,
    val steps: List<RecoveryStep>
)
