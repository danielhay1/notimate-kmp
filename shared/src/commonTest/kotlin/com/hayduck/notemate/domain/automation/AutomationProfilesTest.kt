package com.hayduck.notemate.domain.automation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AutomationProfilesTest {

    @Test
    fun atLeastOneProfileIsRequired() {
        assertFailsWith<IllegalArgumentException> {
            AutomationProfiles(
                profiles = emptyList(),
                selectedProfileId = "personal",
            )
        }
    }

    @Test
    fun selectedProfileMustExist() {
        assertFailsWith<IllegalArgumentException> {
            AutomationProfiles(
                profiles = listOf(profile(id = "personal")),
                selectedProfileId = "work",
            )
        }
    }

    @Test
    fun selectChangesOnlyTheActiveProfile() {
        val personal = profile(id = "personal")
        val work = profile(id = "work")
        val profiles = AutomationProfiles(
            profiles = listOf(personal, work),
            selectedProfileId = personal.id,
        )

        val updated = profiles.select(work.id)

        assertEquals(work, updated.selectedProfile)
        assertEquals(profiles.profiles, updated.profiles)
    }

    private fun profile(id: String): AutomationProfile =
        AutomationProfile(
            id = id,
            name = id,
            automations = listOf(Automation(
                id = "$id-calendar",
                name = "Calendar",
                isEnabled = true,
                sourceSelector = NotificationSourceSelector.AnyMonitoredApplication,
                action = AutomationAction.PROPOSE_CALENDAR_EVENT,
            )),
        )

    @Test
    fun emptyAutomationsAndSharedOwnershipAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            AutomationProfile("empty", "Empty", automations = emptyList())
        }
        val personal = profile("personal")
        val duplicate = AutomationProfile("work", "Work", automations = personal.automations)
        assertFailsWith<IllegalArgumentException> {
            AutomationProfiles(listOf(personal, duplicate), "personal")
        }
    }

    @Test
    fun collectionMutationDoesNotChangeOwnedState() {
        val automations = (profile("personal").automations + profile("work").automations)
            .toMutableList()
        val owned = AutomationProfile("owned", "Owned", automations = automations)
        automations.clear()
        val exposed = owned.automations as MutableList<Automation>
        exposed.clear()
        assertEquals(2, owned.automations.size)
    }
}
