package com.hayduck.notemate.data.configuration

import com.hayduck.notemate.domain.automation.Automation
import com.hayduck.notemate.domain.automation.AutomationProfile
import com.hayduck.notemate.domain.automation.AutomationProfiles
import com.hayduck.notemate.domain.automation.NotificationSourceSelector
import com.hayduck.notemate.domain.configuration.LocalConfiguration

internal fun ConfigurationSnapshot.toDomain(): LocalConfiguration = LocalConfiguration(
    profiles = AutomationProfiles(
        profiles = profiles.sortedBy { it.profile.position }.map { stored ->
            AutomationProfile(
                id = stored.profile.id,
                name = stored.profile.name,
                description = stored.profile.description,
                automations = stored.automations.sortedBy { it.position }.map { it.toDomain() },
            )
        },
        selectedProfileId = settings.selectedProfileId,
    ),
    monitoredApplicationIds = monitoredApplications.map { it.applicationId }.toSet(),
    isPaused = settings.isPaused,
)

private fun AutomationEntity.toDomain(): Automation {
    val selector = if (usesAnyMonitoredApplication) {
        require(applicationId == null) { "Any-monitored selector cannot name an application." }
        NotificationSourceSelector.AnyMonitoredApplication
    } else {
        NotificationSourceSelector.Application(
            requireNotNull(applicationId) { "Application selector requires an identifier." },
        )
    }
    return Automation(
        id = id,
        name = name,
        isEnabled = isEnabled,
        sourceSelector = selector,
        action = action,
        confirmationPolicy = confirmationPolicy,
        conditions = conditions,
        exclusions = exclusions,
    )
}

internal fun Automation.toEntity(profileId: String, position: Int): AutomationEntity =
    AutomationEntity(
        id = id,
        profileId = profileId,
        name = name,
        isEnabled = isEnabled,
        usesAnyMonitoredApplication =
            sourceSelector == NotificationSourceSelector.AnyMonitoredApplication,
        applicationId = (sourceSelector as? NotificationSourceSelector.Application)?.applicationId,
        action = action,
        confirmationPolicy = confirmationPolicy,
        conditions = conditions,
        exclusions = exclusions,
        position = position,
    )
