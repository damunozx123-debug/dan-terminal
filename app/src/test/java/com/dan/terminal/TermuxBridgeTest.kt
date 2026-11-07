package com.dan.terminal

import com.dan.terminal.feature.termux.TermuxRunCommandContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TermuxBridgeTest {

    @Test
    fun bootstrapScriptContainsIdempotentSetupOperations() {
        val script = """
            #!/data/data/com.termux/files/usr/bin/bash
            set -euo pipefail
            mkdir -p "${'$'}HOME/.dan-terminal"/{bin,etc}
            pkg install -y git
        """.trimIndent()

        assertTrue(script.contains("mkdir -p"))
        assertTrue(script.contains("pkg install -y"))
    }

    @Test
    fun shellCommandUsesExecutablePathAndSeparateArguments() {
        val request = TermuxRunCommandContract.shell("printf '%s' ready")

        assertEquals(
            "/data/data/com.termux/files/usr/bin/bash",
            request.executablePath
        )
        assertEquals(listOf("-c", "printf '%s' ready"), request.arguments)
    }
}
