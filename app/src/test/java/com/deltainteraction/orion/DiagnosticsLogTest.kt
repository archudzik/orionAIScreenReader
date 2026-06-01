package com.deltainteraction.orion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DiagnosticsLogTest {

    @Test
    fun redact_removesGoogleApiKeysAndKeyParameters() {
        val googleApiKey = "AIza0123456789abcdefghijklmnopqrstuvwxyz"
        val message = "request failed: apiKey=$googleApiKey&key=another-secret"

        val redacted = DiagnosticsLog.redact(message)

        assertFalse(redacted.contains(googleApiKey))
        assertFalse(redacted.contains("another-secret"))
        assertEquals(
            "request failed: apiKey=[REDACTED]&key=[REDACTED]",
            redacted
        )
    }
}
