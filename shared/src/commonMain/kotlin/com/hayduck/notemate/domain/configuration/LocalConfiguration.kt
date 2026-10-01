package com.hayduck.notemate.domain.configuration

import com.hayduck.notemate.domain.automation.Automation
import com.hayduck.notemate.domain.automation.AutomationAction
import com.hayduck.notemate.domain.automation.AutomationProfile
import com.hayduck.notemate.domain.automation.AutomationProfiles
import com.hayduck.notemate.domain.automation.ConfirmationPolicy
import com.hayduck.notemate.domain.automation.NotificationSourceSelector
import com.hayduck.notemate.domain.notification.NotificationCaptureSettings
import com.hayduck.notemate.domain.notification.NotificationClassification

/** Atomic local configuration snapshot; pausing preserves the selected Profile and Automations. */
class LocalConfiguration(
    val profiles: AutomationProfiles,
    monitoredApplicationIds: Set<String>,
    val isPaused: Boolean,
) {
    private val storedMonitoredApplicationIds = monitoredApplicationIds.toSet()
    val monitoredApplicationIds: Set<String> get() = storedMonitoredApplicationIds.toSet()

    init {
        require(storedMonitoredApplicationIds.all { it.isNotBlank() }) {
            "Monitored application identifiers must not be blank."
        }
    }

    fun asCaptureSettings(): NotificationCaptureSettings = NotificationCaptureSettings(
        monitoredApplicationIds = storedMonitoredApplicationIds,
        profiles = profiles,
        isPaused = isPaused,
    )

    fun selectProfile(profileId: String): LocalConfiguration = LocalConfiguration(
        profiles = profiles.select(profileId),
        monitoredApplicationIds = storedMonitoredApplicationIds,
        isPaused = isPaused,
    )

    fun withPaused(isPaused: Boolean): LocalConfiguration = LocalConfiguration(
        profiles = profiles,
        monitoredApplicationIds = storedMonitoredApplicationIds,
        isPaused = isPaused,
    )

    fun withMonitoredApplications(applicationIds: Set<String>): LocalConfiguration =
        LocalConfiguration(profiles, applicationIds, isPaused)

    /** Updates in place or appends a new Profile; Automation ownership cannot be transferred. */
    fun saveProfile(profile: AutomationProfile): LocalConfiguration {
        val currentProfiles = profiles.profiles
        val updatedProfiles = if (currentProfiles.any { it.id == profile.id }) {
            currentProfiles.map { if (it.id == profile.id) profile else it }
        } else {
            currentProfiles + profile
        }
        return withProfiles(updatedProfiles, profiles.selectedProfileId)
    }

    /** Selected deletion requires a different existing replacement; rejects the only Profile. */
    fun deleteProfile(
        profileId: String,
        replacementProfileId: String? = null,
    ): LocalConfiguration {
        val currentProfiles = profiles.profiles
        requireProfile(profileId)
        require(currentProfiles.size > 1) { "The only Profile cannot be deleted." }
        if (replacementProfileId != null) {
            requireProfile(replacementProfileId)
            require(replacementProfileId != profileId) {
                "The replacement Profile must differ from the deleted Profile."
            }
        }
        val selectedProfileId = if (profiles.selectedProfileId == profileId) {
            requireNotNull(replacementProfileId) {
                "Deleting the selected Profile requires an explicit replacement."
            }
        } else {
            profiles.selectedProfileId
        }
        return withProfiles(currentProfiles.filterNot { it.id == profileId }, selectedProfileId)
    }

    /** Updates in place or appends an Automation to its owner; other owners are never modified. */
    fun saveAutomation(profileId: String, automation: Automation): LocalConfiguration {
        val profile = requireProfile(profileId)
        val currentAutomations = profile.automations
        val updatedAutomations = if (currentAutomations.any { it.id == automation.id }) {
            currentAutomations.map { if (it.id == automation.id) automation else it }
        } else {
            currentAutomations + automation
        }
        return saveProfile(profile.withAutomations(updatedAutomations))
    }

    /** Rejects missing identifiers and deletion of a Profile's final Automation. */
    fun deleteAutomation(profileId: String, automationId: String): LocalConfiguration {
        val profile = requireProfile(profileId)
        require(profile.automations.any { it.id == automationId }) {
            "The Automation must belong to the requested Profile."
        }
        return saveProfile(profile.withAutomations(
            profile.automations.filterNot { it.id == automationId },
        ))
    }

    private fun requireProfile(profileId: String): AutomationProfile =
        requireNotNull(profiles.profiles.find { it.id == profileId }) {
            "The Profile must exist."
        }

    private fun withProfiles(
        updatedProfiles: List<AutomationProfile>,
        selectedProfileId: String,
    ): LocalConfiguration = LocalConfiguration(
        profiles = AutomationProfiles(updatedProfiles, selectedProfileId),
        monitoredApplicationIds = storedMonitoredApplicationIds,
        isPaused = isPaused,
    )

    private fun AutomationProfile.withAutomations(
        automations: List<Automation>,
    ): AutomationProfile =
        AutomationProfile(id, name, description, automations)

    companion object {
        /** Starter configuration admits no source until the user chooses monitored applications. */
        fun initial(): LocalConfiguration = LocalConfiguration(
            profiles = AutomationProfiles(
                profiles = listOf(AutomationProfile(
                    id = "personal",
                    name = "Personal",
                    automations = listOf(Automation(
                        id = "personal-calendar",
                        name = "Calendar event suggestions",
                        isEnabled = true,
                        sourceSelector = NotificationSourceSelector.AnyMonitoredApplication,
                        action = AutomationAction.PROPOSE_CALENDAR_EVENT,
                        confirmationPolicy = ConfirmationPolicy.ALWAYS_REVIEW,
                        conditions = setOf(NotificationClassification.POSSIBLE_CALENDAR_EVENT),
                    )),
                )),
                selectedProfileId = "personal",
            ),
            monitoredApplicationIds = emptySet(),
            isPaused = false,
        )
    }
}
