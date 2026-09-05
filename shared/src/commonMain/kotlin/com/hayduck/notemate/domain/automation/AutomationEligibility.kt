package com.hayduck.notemate.domain.automation

import com.hayduck.notemate.domain.notification.ObservedNotification

fun Automation.isEligible(
    notification: ObservedNotification,
    monitoredApplicationIds: Set<String>,
): Boolean =
    notification.source.applicationId in monitoredApplicationIds &&
        isEnabled &&
        sourceSelector.matches(notification.source)

fun AutomationProfiles.eligibleAutomations(
    notification: ObservedNotification,
    monitoredApplicationIds: Set<String>,
): List<Automation> =
    selectedProfile.automations.filter { automation ->
        automation.isEligible(
            notification = notification,
            monitoredApplicationIds = monitoredApplicationIds,
        )
    }
