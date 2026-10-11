package com.hayduck.notemate.featurekeys

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationTestUiStateTest {
    @Test
    fun requiresBoundedNonBlankInputAndRedactsDiagnostics() {
        assertFalse(NotificationTestUiState().isValid)
        assertFalse(NotificationTestUiState(" ", "Synthetic body").isValid)
        assertFalse(NotificationTestUiState("x".repeat(257), "Synthetic body").isValid)
        assertFalse(NotificationTestUiState("Synthetic title", "x".repeat(4097)).isValid)
        assertTrue(NotificationTestUiState("x".repeat(256), "x".repeat(4096)).isValid)
        assertFalse(NotificationTestUiState("Synthetic title", "Synthetic body")
            .toString().contains("Synthetic"))
    }
}
