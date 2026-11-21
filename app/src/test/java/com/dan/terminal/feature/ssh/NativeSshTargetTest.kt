package com.dan.terminal.feature.ssh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NativeSshTargetTest {
    private val valid = NativeSshTarget(
        host = "100.72.159.123",
        port = 22,
        username = "dan",
        hostFingerprint = "SHA256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    )

    @Test fun validPinnedHostIsAllowed() {
        assertNull(valid.validate())
    }

    @Test fun emptyFingerprintIsRejected() {
        assertNotNull(valid.copy(hostFingerprint = "").validate())
    }

    @Test fun md5FingerprintIsNotAccepted() {
        assertNotNull(valid.copy(hostFingerprint = "00:11:22:33").validate())
    }

    @Test fun invalidPortIsRejected() {
        assertNotNull(valid.copy(port = 0).validate())
        assertNotNull(valid.copy(port = 65536).validate())
    }

    @Test fun usernameCannotContainWhitespace() {
        assertNotNull(valid.copy(username = "dan root").validate())
    }

    @Test fun literalSha256FingerprintFormatIsRequired() {
        assertEquals(null, valid.copy(
            hostFingerprint = "SHA256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa="
        ).validate())
    }
}
