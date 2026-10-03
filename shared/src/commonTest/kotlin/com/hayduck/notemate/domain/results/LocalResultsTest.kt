package com.hayduck.notemate.domain.results

import com.hayduck.notemate.domain.activity.ActivityEvent
import com.hayduck.notemate.domain.activity.ActivityRecord
import com.hayduck.notemate.domain.proposal.CalendarDate
import com.hayduck.notemate.domain.proposal.CalendarField
import com.hayduck.notemate.domain.proposal.CalendarFields
import com.hayduck.notemate.domain.proposal.CalendarProposal
import com.hayduck.notemate.domain.proposal.CalendarTime
import com.hayduck.notemate.domain.proposal.ProposalExplanation
import com.hayduck.notemate.domain.proposal.ProposalOrigin
import com.hayduck.notemate.domain.proposal.ProposalState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame

class LocalResultsTest {
    private val origin = ProposalOrigin("profile", "Synthetic profile", "automation",
        "Synthetic automation", "example.app", "Synthetic application")
    private val fields = CalendarFields("Synthetic event", CalendarDate(2026, 10, 3),
        CalendarTime(18, 0), "Synthetic location")
    private fun draft(expiry: Long? = 200) = CalendarProposal.draft(
        "proposal", origin, fields, 100, expiry,
    )

    @Test
    fun restorationPreservesEveryReachableLifecycleAndExplanation() {
        val initial = draft()
        val edited = initial.edit(fields.copy(title = "Edited synthetic event"), emptySet(), 110)
        val states = listOf(initial, initial.validate(110),
            initial.edit(fields.copy(time = null), setOf(CalendarField.TIME), 110),
            edited, edited.dismiss(120), edited.markHandedOff(120), edited.defer(120),
            edited.fail(120), edited.expire(200))
        for (expected in states) {
            val restored = restore(expected)
            assertEquals(expected.id, restored.id)
            assertEquals(expected.fields, restored.fields)
            assertEquals(expected.origin, restored.origin)
            assertEquals(expected.ambiguousFields, restored.ambiguousFields)
            assertEquals(expected.explanation, restored.explanation)
            assertEquals(expected.state, restored.state)
            assertEquals(expected.createdAtEpochMilliseconds, restored.createdAtEpochMilliseconds)
            assertEquals(expected.expiresAtEpochMilliseconds, restored.expiresAtEpochMilliseconds)
        }
    }

    @Test
    fun restorationRejectsImpossibleStatesWithoutEchoingStructuredContent() {
        val invalidReady = CalendarProposal.draft("proposal", origin,
            fields.copy(time = null), 100)
        assertFailsWith<IllegalArgumentException> {
            restore(invalidReady, ProposalState.READY)
        }
        assertFailsWith<IllegalArgumentException> {
            restore(invalidReady, ProposalState.HANDED_OFF)
        }
        assertFailsWith<IllegalArgumentException> { restore(draft(), ProposalState.NEEDS_REVIEW) }
        assertFailsWith<IllegalArgumentException> { restore(draft(null), ProposalState.EXPIRED) }
        assertFailsWith<IllegalArgumentException> {
            restore(draft(), explanation = ProposalExplanation.USER_CORRECTED_FIELDS)
        }
    }

    @Test
    fun commandsIncrementRevisionAndRejectStaleOrBackwardsChanges() {
        val initial = draft().asNewStoredProposal()
        val ready = initial.applyChange(0, ProposalChange.Validate, 110)
        assertEquals(1L, ready.revision)
        assertEquals(110L, ready.updatedAtEpochMilliseconds)
        assertEquals(ProposalState.READY, ready.proposal.state)
        assertFailsWith<IllegalStateException> {
            ready.applyChange(0, ProposalChange.Dismiss, 120)
        }
        assertFailsWith<IllegalArgumentException> {
            ready.applyChange(1, ProposalChange.Edit(fields, emptySet()), 109)
        }
        val dismissed = ready.applyChange(1, ProposalChange.Dismiss, 120)
        assertFailsWith<IllegalStateException> {
            dismissed.applyChange(2, ProposalChange.Validate, 130)
        }
    }

    @Test
    fun expiryRetainsFieldsAndCreatesNoRevisionUntilDeadline() {
        val initial = draft().asNewStoredProposal()
        assertSame(initial, initial.applyChange(0, ProposalChange.Expire, 199))
        val expired = initial.applyChange(0, ProposalChange.Expire, 200)
        assertEquals(ProposalState.EXPIRED, expired.proposal.state)
        assertEquals(initial.proposal.fields, expired.proposal.fields)
        assertEquals(1L, expired.revision)
        assertSame(expired, expired.applyChange(1, ProposalChange.Expire, 300))
        val indefinite = draft(null).asNewStoredProposal()
        assertSame(indefinite, indefinite.applyChange(0, ProposalChange.Expire, Long.MAX_VALUE))
    }

    @Test
    fun initialStateAndStoredTimestampValidationRejectInvalidRecords() {
        assertFailsWith<IllegalArgumentException> { draft().dismiss(100).asNewStoredProposal() }
        assertFailsWith<IllegalArgumentException> { StoredCalendarProposal(draft(), -1, 100) }
        assertFailsWith<IllegalArgumentException> { StoredCalendarProposal(draft(), 0, 99) }
        assertFailsWith<IllegalArgumentException> {
            StoredCalendarProposal(draft().expire(200), 1, 199)
        }
        assertFailsWith<IllegalArgumentException> { StoredCalendarProposal(draft(), 1, 200) }
        assertFailsWith<IllegalStateException> {
            StoredCalendarProposal(draft(), Long.MAX_VALUE, 100)
                .applyChange(Long.MAX_VALUE, ProposalChange.Validate, 110)
        }
    }

    @Test
    fun generatedActivityContainsOnlyOriginIdsAndTypedEvent() {
        val proposal = draft()
        val activity = proposal.activityRecord("activity", ProposalChange.Edit(fields,
            emptySet()).activityEvent, 110)
        assertEquals(ActivityRecord("activity", ActivityEvent.PROPOSAL_EDITED, 110,
            "proposal", "profile", "automation", "example.app"), activity)
        val outputs = listOf(activity.toString(), LocalResults(
            listOf(proposal.asNewStoredProposal()), listOf(activity)).toString(),
            ProposalChange.Edit(fields, emptySet()).toString(),
            proposal.asNewStoredProposal().toString())
        assertFalse(outputs.any { "Synthetic" in it })
        for (event in ActivityEvent.entries) {
            val standalone = event in setOf(ActivityEvent.NOTIFICATION_IGNORED,
                ActivityEvent.PROFILE_ACTIVATED, ActivityEvent.NOTIFICATION_ACCESS_LOST)
            val record = ActivityRecord("activity", event, 100)
            if (standalone) record.requireStandaloneEvent() else {
                assertFailsWith<IllegalArgumentException> { record.requireStandaloneEvent() }
            }
        }
        assertFailsWith<IllegalArgumentException> { activity.requireStandaloneEvent() }
    }

    @Test
    fun snapshotAndEditDefensivelyOwnInputCollections() {
        val proposals = mutableListOf(draft().asNewStoredProposal())
        val activity = mutableListOf(ActivityRecord("activity", ActivityEvent.PROFILE_ACTIVATED, 100))
        val snapshot = LocalResults(proposals, activity)
        proposals.clear()
        activity.clear()
        assertEquals(1, snapshot.proposals.size)
        assertEquals(1, snapshot.activity.size)
        val ambiguous = mutableSetOf(CalendarField.TIME)
        val edit = ProposalChange.Edit(fields, ambiguous)
        ambiguous.clear()
        assertEquals(setOf(CalendarField.TIME), edit.ambiguousFields)
        assertFailsWith<IllegalArgumentException> {
            LocalResults(snapshot.proposals + snapshot.proposals, emptyList())
        }
        assertFailsWith<IllegalArgumentException> {
            LocalResults(emptyList(), snapshot.activity + snapshot.activity)
        }
    }

    private fun restore(
        proposal: CalendarProposal,
        state: ProposalState = proposal.state,
        explanation: ProposalExplanation = proposal.explanation,
    ): CalendarProposal = CalendarProposal.restore(proposal.id, proposal.origin, proposal.fields,
        proposal.ambiguousFields, explanation, state, proposal.createdAtEpochMilliseconds,
        proposal.expiresAtEpochMilliseconds)
}
