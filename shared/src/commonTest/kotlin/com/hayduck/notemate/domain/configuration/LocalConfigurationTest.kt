package com.hayduck.notemate.domain.configuration

import com.hayduck.notemate.domain.automation.Automation
import com.hayduck.notemate.domain.automation.AutomationAction
import com.hayduck.notemate.domain.automation.AutomationProfile
import com.hayduck.notemate.domain.automation.AutomationProfiles
import com.hayduck.notemate.domain.automation.ConfirmationPolicy
import com.hayduck.notemate.domain.automation.NotificationSourceSelector
import com.hayduck.notemate.domain.notification.NotificationClassification
import com.hayduck.notemate.domain.notification.NotificationSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalConfigurationTest {
    @Test
    fun initialConfigurationRequiresSourceOptInAndReview() {
        val configuration = LocalConfiguration.initial()
        val profile = configuration.profiles.selectedProfile
        val automation = profile.automations.single()

        assertEquals("personal", profile.id)
        assertEquals("Personal", profile.name)
        assertEquals("personal-calendar", automation.id)
        assertEquals("Calendar event suggestions", automation.name)
        assertTrue(automation.isEnabled)
        assertEquals(NotificationSourceSelector.AnyMonitoredApplication, automation.sourceSelector)
        assertEquals(AutomationAction.PROPOSE_CALENDAR_EVENT, automation.action)
        assertEquals(ConfirmationPolicy.ALWAYS_REVIEW, automation.confirmationPolicy)
        assertEquals(
            setOf(NotificationClassification.POSSIBLE_CALENDAR_EVENT),
            automation.conditions,
        )
        assertTrue(configuration.monitoredApplicationIds.isEmpty())
        assertFalse(configuration.isPaused)
        assertFalse(configuration.asCaptureSettings().admits(source))
    }

    @Test
    fun monitoredApplicationsAreDefensiveAndRejectBlankIdentifiers() {
        val monitoredIds = mutableSetOf("synthetic.chat", "synthetic.calendar")
        val configuration = LocalConfiguration.initial().withMonitoredApplications(monitoredIds)
        monitoredIds.clear()
        (configuration.monitoredApplicationIds as MutableSet<String>).clear()

        assertEquals(
            setOf("synthetic.chat", "synthetic.calendar"),
            configuration.monitoredApplicationIds,
        )
        assertTrue(configuration.asCaptureSettings().admits(source))
        assertFailsWith<IllegalArgumentException> {
            configuration.withMonitoredApplications(setOf("synthetic.chat", " "))
        }
    }

    @Test
    fun pauseAndProfileSelectionPreserveIndependentSettings() {
        val initial = LocalConfiguration.initial()
            .saveProfile(profile("work", automation("work-calendar")))
            .withMonitoredApplications(setOf(source.applicationId))
        val paused = initial.withPaused(true).selectProfile("work")

        assertTrue(paused.isPaused)
        assertEquals("work", paused.profiles.selectedProfileId)
        assertEquals(initial.monitoredApplicationIds, paused.monitoredApplicationIds)
        assertFalse(paused.asCaptureSettings().admits(source))
        val resumed = paused.withPaused(false)
        assertEquals("work", resumed.profiles.selectedProfileId)
        assertTrue(resumed.asCaptureSettings().admits(source))
        assertEquals("personal", initial.profiles.selectedProfileId)
        assertFalse(initial.isPaused)
    }

    @Test
    fun savesReplaceInPlaceAndAppendWithoutChangingSelection() {
        val work = profile("work", automation("work-calendar"), automation("work-ignore"))
        val original = LocalConfiguration.initial().saveProfile(work)
        val renamed = AutomationProfile(
            work.id, "Renamed", "Synthetic description", work.automations,
        )
        val replacement = automation("work-calendar", "Changed")
        val updated = original.saveProfile(renamed)
            .saveAutomation("work", replacement)
            .saveAutomation("work", automation("work-review"))
        val updatedWork = updated.profiles.profiles.last()

        assertEquals(listOf("personal", "work"), updated.profiles.profiles.map { it.id })
        assertEquals("personal", updated.profiles.selectedProfileId)
        assertEquals("Renamed", updatedWork.name)
        assertEquals("Synthetic description", updatedWork.description)
        assertEquals(
            listOf("work-calendar", "work-ignore", "work-review"),
            updatedWork.automations.map { it.id },
        )
        assertEquals(replacement, updatedWork.automations.first())
        assertEquals(work, original.profiles.profiles.last())
    }

    @Test
    fun foreignAutomationOwnershipCannotBeTransferredByEitherSave() {
        val initial = LocalConfiguration.initial()
            .saveProfile(profile("work", automation("work-calendar")))
        val foreignAutomation = initial.profiles.selectedProfile.automations.single()

        assertFailsWith<IllegalArgumentException> {
            initial.saveAutomation("work", foreignAutomation)
        }
        assertFailsWith<IllegalArgumentException> {
            initial.saveProfile(profile("work", foreignAutomation))
        }
        assertFailsWith<IllegalArgumentException> {
            initial.saveProfile(profile("new", foreignAutomation))
        }
        assertEquals(
            listOf("work-calendar"),
            initial.profiles.profiles.last().automations.map { it.id },
        )
    }

    @Test
    fun selectedProfileDeletionRequiresExistingDifferentReplacement() {
        val initial = LocalConfiguration.initial()
            .saveProfile(profile("work", automation("work-calendar")))
        assertFailsWith<IllegalArgumentException> { initial.deleteProfile("personal") }
        assertFailsWith<IllegalArgumentException> { initial.deleteProfile("personal", "personal") }
        assertFailsWith<IllegalArgumentException> { initial.deleteProfile("personal", "missing") }

        val updated = initial.deleteProfile("personal", "work")
        assertEquals("work", updated.profiles.selectedProfileId)
        assertEquals(listOf("work"), updated.profiles.profiles.map { it.id })
        assertEquals("personal", initial.profiles.selectedProfileId)
    }

    @Test
    fun deletingUnselectedProfilePreservesSelectionPauseAndSources() {
        val initial = LocalConfiguration.initial()
            .saveProfile(profile("work", automation("work-calendar")))
            .withPaused(true)
            .withMonitoredApplications(setOf(source.applicationId))
        val updated = initial.deleteProfile("work")

        assertEquals("personal", updated.profiles.selectedProfileId)
        assertTrue(updated.isPaused)
        assertEquals(initial.monitoredApplicationIds, updated.monitoredApplicationIds)
        assertFailsWith<IllegalArgumentException> { updated.deleteProfile("personal") }
    }

    @Test
    fun deletingAutomationPreservesOwnerMetadataAndOrder() {
        val initial = LocalConfiguration.initial().saveProfile(
            AutomationProfile(
                id = "work",
                name = "Work",
                description = "Synthetic description",
                automations = listOf(
                    automation("first"), automation("second"), automation("third"),
                ),
            ),
        )
        val updated = initial.deleteAutomation("work", "second")
        val owner = updated.profiles.profiles.last()

        assertEquals("Work", owner.name)
        assertEquals("Synthetic description", owner.description)
        assertEquals(listOf("first", "third"), owner.automations.map { it.id })
        assertEquals("personal", updated.profiles.selectedProfileId)
        assertFailsWith<IllegalArgumentException> {
            updated.deleteAutomation("personal", "personal-calendar")
        }
    }

    @Test
    fun operationsRejectMissingIdentifiersAndWrongAutomationOwner() {
        val initial = LocalConfiguration.initial()
            .saveProfile(profile("work", automation("work-calendar")))

        assertFailsWith<IllegalArgumentException> { initial.selectProfile("missing") }
        assertFailsWith<IllegalArgumentException> { initial.deleteProfile("missing") }
        assertFailsWith<IllegalArgumentException> {
            initial.saveAutomation("missing", automation("new"))
        }
        assertFailsWith<IllegalArgumentException> {
            initial.deleteAutomation("missing", "personal-calendar")
        }
        assertFailsWith<IllegalArgumentException> {
            initial.deleteAutomation("personal", "missing")
        }
        assertFailsWith<IllegalArgumentException> {
            initial.deleteAutomation("personal", "work-calendar")
        }
    }

    @Test
    fun captureSnapshotPreservesSelectedSourceSelectorAndEnabledGate() {
        val disabled = Automation(
            id = "work-calendar",
            name = "Disabled",
            isEnabled = false,
            sourceSelector = NotificationSourceSelector.AnyMonitoredApplication,
            action = AutomationAction.PROPOSE_CALENDAR_EVENT,
        )
        val restricted = Automation(
            id = "restricted-calendar",
            name = "Restricted",
            isEnabled = true,
            sourceSelector = NotificationSourceSelector.Application("synthetic.other"),
            action = AutomationAction.PROPOSE_CALENDAR_EVENT,
        )
        val configuration = LocalConfiguration(
            AutomationProfiles(listOf(profile("work", disabled, restricted)), "work"),
            setOf(source.applicationId),
            false,
        )

        assertFalse(configuration.asCaptureSettings().admits(source))
    }

    private fun profile(id: String, vararg automations: Automation): AutomationProfile =
        AutomationProfile(id, id, automations = automations.toList())

    private fun automation(id: String, name: String = id): Automation = Automation(
        id = id,
        name = name,
        isEnabled = true,
        sourceSelector = NotificationSourceSelector.AnyMonitoredApplication,
        action = AutomationAction.PROPOSE_CALENDAR_EVENT,
    )

    private val source = NotificationSource("synthetic.chat")
}
