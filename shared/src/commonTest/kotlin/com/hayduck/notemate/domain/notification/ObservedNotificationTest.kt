package com.hayduck.notemate.domain.notification

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class ObservedNotificationTest {

    @Test
    fun `string representations redact notification content`() {
        val privateTitle = "Synthetic private title"
        val privateBody = "Synthetic private body"
        val notification = ObservedNotification(
            source = NotificationSource(applicationId = "com.example.synthetic"),
            postedAtEpochMilliseconds = 1_000,
            content = NotificationContent(
                title = privateTitle,
                body = privateBody,
            ),
        )

        val rendered = listOf(notification, notification.content).joinToString()

        assertContains(rendered, "[REDACTED]")
        assertFalse(privateTitle in rendered)
        assertFalse(privateBody in rendered)
    }
}
