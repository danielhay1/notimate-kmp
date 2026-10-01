package com.hayduck.notemate.domain.automation

/** An owned, nonempty set of Automations; caller collections are copied on construction. */
class AutomationProfile(
    val id: String,
    val name: String,
    val description: String? = null,
    automations: List<Automation>,
) {
    private val storedAutomations = automations.toList()
    val automations: List<Automation> get() = storedAutomations.toList()

    init {
        require(id.isNotBlank()) { "Profile identifier must not be blank." }
        require(name.isNotBlank()) { "Profile name must not be blank." }
        require(this.automations.isNotEmpty()) { "A Profile must contain an Automation." }
        require(this.automations.map(Automation::id).distinct().size == this.automations.size) {
            "Automation identifiers must be unique."
        }
    }
}

class AutomationProfiles(
    profiles: List<AutomationProfile>,
    val selectedProfileId: String,
) {
    private val storedProfiles = profiles.toList()
    val profiles: List<AutomationProfile> get() = storedProfiles.toList()

    init {
        require(this.profiles.isNotEmpty()) { "At least one Profile is required." }
        require(this.profiles.map(AutomationProfile::id).distinct().size == this.profiles.size) {
            "Profile identifiers must be unique."
        }
        require(this.profiles.any { it.id == selectedProfileId }) {
            "The selected Profile must exist."
        }
        val automationIds = this.profiles.flatMap { it.automations }.map(Automation::id)
        require(automationIds.distinct().size == automationIds.size) {
            "Each Automation must belong to exactly one Profile."
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
