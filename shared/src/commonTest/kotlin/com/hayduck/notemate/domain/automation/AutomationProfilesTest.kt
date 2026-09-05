package com.hayduck.notemate.domain.automation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AutomationProfilesTest {

    @Test
    fun `at least one Profile is required`() {
        assertFailsWith<IllegalArgumentException> {
            AutomationProfiles(
                profiles = emptyList(),
                selectedProfileId = "personal",
            )
        }
    }

    @Test
    fun `selected Profile must exist`() {
        assertFailsWith<IllegalArgumentException> {
            AutomationProfiles(
                profiles = listOf(profile(id = "personal")),
                selectedProfileId = "work",
            )
        }
    }

    @Test
    fun `select changes only the active Profile`() {
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
        )
}
