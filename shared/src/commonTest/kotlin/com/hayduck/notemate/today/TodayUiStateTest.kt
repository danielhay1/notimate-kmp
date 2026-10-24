package com.hayduck.notemate.today

import com.hayduck.notemate.domain.activity.ActivityEvent
import com.hayduck.notemate.domain.activity.ActivityRecord
import com.hayduck.notemate.domain.proposal.ProposalState
import com.hayduck.notemate.domain.results.LocalResults
import com.hayduck.notemate.domain.results.StoredCalendarProposal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TodayUiStateTest {
    @Test
    fun processingUsesAccessConfigurationAndPauseIndependentlyOfOutgoingAlerts() {
        val configuration = TodayConfigurationUiState.Ready("Synthetic Profile", false, 1, 1)
        val granted = TodayUiState(
            access = NotificationAccessState.GRANTED,
            configuration = configuration,
        )
        ProposalAlertPermission.entries.forEach { alerts ->
            assertEquals(TodayProcessingState.PROCESSOR_UNAVAILABLE,
                granted.copy(alerts = alerts).processing)
        }
        assertEquals(TodayProcessingState.PERMISSION_REQUIRED,
            granted.copy(access = NotificationAccessState.REVOKED).processing)
        assertEquals(TodayProcessingState.PAUSED,
            granted.copy(configuration = configuration.copy(isPaused = true)).processing)
        assertEquals(TodayProcessingState.CONFIGURATION_UNAVAILABLE,
            granted.copy(configuration = TodayConfigurationUiState.Failed).processing)
        assertEquals(TodayProcessingState.CHECKING,
            granted.copy(configuration = TodayConfigurationUiState.Loading).processing)
        assertEquals(TodayProcessingState.CHECKING,
            granted.copy(access = NotificationAccessState.CHECKING).processing)
    }

    @Test
    fun queueExcludesCompletedOutcomesAndSortsByCreationWithoutChangingStoredData() {
        val older = syntheticProposal("older", 100)
        val newerB = syntheticProposal("newer-b", 200)
        val newerA = syntheticProposal("newer-a", 200)
        val dismissed = syntheticProposal("dismissed").let {
            StoredCalendarProposal(it.proposal.dismiss(100), 1, 100)
        }
        val handedOff = syntheticProposal("handed-off").let {
            StoredCalendarProposal(it.proposal.markHandedOff(100), 1, 100)
        }
        val results = LocalResults(listOf(older, newerB, dismissed, newerA, handedOff), emptyList())
        val projected = results.toTodayUiState(1_000)
        assertEquals(listOf("newer-a", "newer-b", "older"), projected.proposals.map { it.id })
        assertEquals(5, results.proposals.size)
        assertEquals(ProposalState.DISMISSED, dismissed.proposal.state)
        assertEquals(ProposalState.HANDED_OFF, handedOff.proposal.state)
    }

    @Test
    fun recentOutcomesAreTheLatestFiveTypedEventsWithStableTieOrdering() {
        val events = listOf(
            ActivityRecord("oldest", ActivityEvent.NOTIFICATION_IGNORED, 100),
            ActivityRecord("tied-b", ActivityEvent.PROFILE_ACTIVATED, 200),
            ActivityRecord("tied-a", ActivityEvent.NOTIFICATION_ACCESS_LOST, 200),
            ActivityRecord("third", ActivityEvent.CALENDAR_PROPOSAL_CREATED, 300),
            ActivityRecord("fourth", ActivityEvent.PROPOSAL_DISMISSED, 400),
            ActivityRecord("fifth", ActivityEvent.OPENED_IN_CALENDAR, 500),
            ActivityRecord("sixth", ActivityEvent.PROPOSAL_DEFERRED, 600),
        )
        val results = LocalResults(emptyList(), events)
        val projected = results.toTodayUiState(1_000)
        assertEquals(listOf("sixth", "fifth", "fourth", "third", "tied-a"),
            projected.recentOutcomes.map { it.id })
        assertEquals(ActivityEvent.NOTIFICATION_ACCESS_LOST, projected.recentOutcomes.last().event)
        assertEquals(events, results.activity)
    }

    @Test
    fun expiryAndBackwardsClockOnlyAffectDisplayWithoutChangingStoredProposal() {
        val stored = syntheticProposal(expiresAt = 2_000)
        val results = LocalResults(listOf(stored), emptyList())
        assertEquals(ProposalState.READY,
            results.toTodayUiState(1_999).proposals.single().state)
        assertEquals(ProposalState.EXPIRED,
            results.toTodayUiState(2_000).proposals.single().state)
        assertEquals(ProposalState.READY,
            results.toTodayUiState(50).proposals.single().state)
        assertEquals(ProposalState.READY, stored.proposal.state)
        assertEquals(0L, stored.revision)
        assertTrue(results.activity.isEmpty())
    }

    @Test
    fun proposalStringRepresentationDoesNotExposeStructuredFieldsOrOriginLabels() {
        val projected = LocalResults(listOf(syntheticProposal()), emptyList())
            .toTodayUiState(1_000).proposals.single()
        assertFalse(projected.toString().contains("Synthetic"))
        assertTrue(projected.toString().contains("REDACTED"))
    }
}
