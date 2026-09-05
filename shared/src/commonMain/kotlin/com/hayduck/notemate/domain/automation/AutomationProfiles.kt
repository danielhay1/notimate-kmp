package com.hayduck.notemate.domain.automation

data class AutomationProfile(
    val id: String,
    val name: String,
    val description: String? = null,
    val automations: List<Automation> = emptyList(),
) {
    init {
        require(id.isNotBlank()) { "Profile identifier must not be blank." }
        require(name.isNotBlank()) { "Profile name must not be blank." }
    }
}

class AutomationProfiles(
    profiles: List<AutomationProfile>,
    val selectedProfileId: String,
) {
    val profiles: List<AutomationProfile> = profiles.toList()

    init {
        require(this.profiles.isNotEmpty()) { "At least one Profile is required." }
        require(this.profiles.map(AutomationProfile::id).distinct().size == this.profiles.size) {
            "Profile identifiers must be unique."
        }
        require(this.profiles.any { it.id == selectedProfileId }) {
            "The selected Profile must exist."
        }
    }

    val selectedProfile: AutomationProfile
        get() = profiles.first { it.id == selectedProfileId }

    fun select(profileId: String): AutomationProfiles =
        AutomationProfiles(
            profiles = profiles,
            selectedProfileId = profileId,
        )
}
