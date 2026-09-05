package com.hayduck.notemate.domain.automation

import com.hayduck.notemate.domain.notification.NotificationContent
import com.hayduck.notemate.domain.notification.NotificationSource
import com.hayduck.notemate.domain.notification.ObservedNotification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AutomationEligibilityTest {

    @Test
    fun `eligible automations come only from the selected Profile and satisfy every rule`() {
        val notification = observedNotification(applicationId = "com.example.calendar-source")
        val matchingAutomation = automation(
            id = "matching",
            applicationId = notification.source.applicationId,
        )
        val profiles = AutomationProfiles(
            profiles = listOf(
                AutomationProfile(
                    id = "personal",
                    name = "Personal",
                    automations = listOf(
                        matchingAutomation,
                        automation(
                            id = "disabled",
                            applicationId = notification.source.applicationId,
                            isEnabled = false,
                        ),
                        automation(
                            id = "different-source",
                            applicationId = "com.example.different",
                        ),
                    ),
                ),
                AutomationProfile(
                    id = "work",
                    name = "Work",
                    automations = listOf(
                        automation(
                            id = "unselected-profile",
                            applicationId = notification.source.applicationId,
                        ),
                    ),
                ),
            ),
            selectedProfileId = "personal",
        )

        val eligible = profiles.eligibleAutomations(
            notification = notification,
            monitoredApplicationIds = setOf(notification.source.applicationId),
        )

        assertEquals(listOf(matchingAutomation), eligible)
    }

    @Test
    fun `globally unmonitored notification is ineligible for any source selector`() {
        val notification = observedNotification(applicationId = "com.example.unmonitored")
        val anySourceAutomation = Automation(
            id = "any-source",
            name = "Any monitored source",
            isEnabled = true,
            sourceSelector = NotificationSourceSelector.AnyMonitoredApplication,
            action = AutomationAction.PROPOSE_CALENDAR_EVENT,
        )

        assertFalse(
            actual = anySourceAutomation.isEligible(
                notification = notification,
                monitoredApplicationIds = emptySet(),
            ),
        )
    }

    private fun automation(
        id: String,
        applicationId: String,
        isEnabled: Boolean = true,
    ): Automation = Automation(
        id = id,
        name = id,
        isEnabled = isEnabled,
        sourceSelector = NotificationSourceSelector.Application(applicationId),
        action = AutomationAction.PROPOSE_CALENDAR_EVENT,
    )

    private fun observedNotification(applicationId: String): ObservedNotification =
        ObservedNotification(
            source = NotificationSource(applicationId = applicationId),
            postedAtEpochMilliseconds = 1_000,
            content = NotificationContent(
                title = "Synthetic event",
                body = "Synthetic event tomorrow at 10:00",
            ),
        )
}
