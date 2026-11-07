package com.dan.terminal.core.shizuku

import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.mock

class ShizukuManagerTest {
    private val api = FakeShizukuApi()
    private val manager = ShizukuManager(mock(Context::class.java), api).also { it.init() }

    @Test fun stoppedServiceDoesNotRequestPermission() {
        manager.requestPermission()
        assertEquals(0, api.requests)
        assertFalse(manager.state.value.ready)
        assertNotNull(manager.state.value.lastError)
    }

    @Test fun rechecksBinderBeforeUsingPreviouslyGrantedState() {
        api.alive = true
        api.granted = true
        manager.refresh()
        assertTrue(manager.state.value.ready)
        api.alive = false
        manager.requestPermission()
        assertFalse(manager.state.value.permissionGranted)
        assertEquals(0, api.requests)
    }

    @Test fun preV11DoesNotCallModernPermissionApi() {
        api.alive = true
        api.legacy = true
        manager.requestPermission()
        assertTrue(manager.state.value.preV11)
        assertEquals(0, api.permissionChecks)
        assertEquals(0, api.requests)
    }

    @Test fun permanentDenialDirectsUserToManagerWithoutRequestingAgain() {
        api.alive = true
        api.blocked = true
        manager.requestPermission()
        assertTrue(manager.state.value.permissionBlocked)
        assertEquals(0, api.requests)
        assertTrue(manager.state.value.lastError!!.contains("Open Shizuku"))
    }

    @Test fun repeatedTapDoesNotCreateConcurrentRequests() {
        api.alive = true
        manager.requestPermission()
        manager.requestPermission()
        assertEquals(1, api.requests)
        assertTrue(manager.state.value.permissionRequestPending)
    }

    @Test fun denialIsVisibleAndGrantClearsErrorAfterRecheck() {
        api.alive = true
        manager.requestPermission()
        api.result(4001, false)
        assertFalse(manager.state.value.permissionRequestPending)
        assertFalse(manager.state.value.ready)
        assertTrue(manager.state.value.lastError!!.contains("denied"))
        manager.requestPermission()
        api.granted = true
        api.result(4001, true)
        assertTrue(manager.state.value.ready)
        assertNull(manager.state.value.lastError)
    }

    @Test fun unrelatedResultDoesNotCompletePendingRequest() {
        api.alive = true
        manager.requestPermission()
        api.result(9999, true)
        assertTrue(manager.state.value.permissionRequestPending)
        assertFalse(manager.state.value.ready)
    }

    @Test fun binderDeathClearsGrantAndPendingStateImmediately() {
        api.alive = true
        api.granted = true
        manager.refresh()
        api.dead()
        assertFalse(manager.state.value.ready)
        assertFalse(manager.state.value.permissionGranted)
        assertFalse(manager.state.value.permissionRequestPending)
    }

    @Test fun requestExceptionFailsClosedAndAllowsRecheck() {
        api.alive = true
        api.failRequest = true
        manager.requestPermission()
        assertFalse(manager.state.value.ready)
        assertFalse(manager.state.value.permissionRequestPending)
        assertNotNull(manager.state.value.lastError)
    }

    @Test fun listenerRegistrationIsIdempotentAndCleanedUp() {
        manager.init()
        assertEquals(1, api.subscriptions)
        manager.destroy()
        manager.destroy()
        assertEquals(1, api.unsubscriptions)
        assertFalse(manager.state.value.ready)
    }

    @Test fun explicitRecheckRecoversFromMissingResultCallback() {
        api.alive = true
        manager.requestPermission()
        manager.recheckPermission()
        assertFalse(manager.state.value.permissionRequestPending)
        assertFalse(manager.state.value.ready)
    }

    @Test fun listenerFailurePreventsRequestWithoutResultObserver() {
        manager.destroy()
        api.alive = true
        api.failSubscribe = true
        manager.requestPermission()
        assertEquals(0, api.requests)
        assertNotNull(manager.state.value.lastError)
    }
}

private class FakeShizukuApi : ShizukuApi {
    var alive = false
    var granted = false
    var legacy = false
    var blocked = false
    var failRequest = false
    var failSubscribe = false
    var requests = 0
    var permissionChecks = 0
    var subscriptions = 0
    var unsubscriptions = 0
    var dead: () -> Unit = {}
    var result: (Int, Boolean) -> Unit = { _, _ -> }
    override fun pingBinder() = alive
    override fun isPreV11() = legacy
    override fun version() = if (legacy) 10 else 13
    override fun permissionGranted(): Boolean { permissionChecks++; return granted }
    override fun shouldShowRationale() = blocked
    override fun requestPermission(code: Int) {
        check(!failRequest) { "Service stopped during request" }
        requests++
    }
    override fun subscribe(
        onReceived: () -> Unit,
        onDead: () -> Unit,
        onResult: (Int, Boolean) -> Unit
    ): () -> Unit {
        check(!failSubscribe) { "Listener registration failed" }
        subscriptions++
        dead = onDead
        result = onResult
        return { unsubscriptions++ }
    }
}
