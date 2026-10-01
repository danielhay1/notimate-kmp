package com.hayduck.notemate.domain.proposal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CalendarProposalTest {
    private val origin = ProposalOrigin("profile", "Sample profile", "automation",
        "Sample automation", "example.app", "Sample application")
    private val fields = CalendarFields("Synthetic event", CalendarDate(2026, 10, 2),
        CalendarTime(18, 0), "Synthetic location")

    private fun draft(ambiguity: Set<CalendarField> = emptySet()) = CalendarProposal.draft(
        "proposal", origin, fields, 100, 200, ambiguity,
    )

    @Test
    fun missingAndAmbiguousFieldsRequireReview() {
        val incomplete = CalendarProposal.draft("proposal", origin,
            CalendarFields(" ", null, null), 100).validate(100)
        assertEquals(ProposalState.NEEDS_REVIEW, incomplete.state)
        assertEquals(setOf(ProposalIssue.MISSING_TITLE, ProposalIssue.MISSING_DATE,
            ProposalIssue.MISSING_TIME), incomplete.issues)
        assertFailsWith<IllegalStateException> { incomplete.markHandedOff(100) }
        assertEquals(ProposalState.NEEDS_REVIEW,
            draft(setOf(CalendarField.TIME)).validate(100).state)
    }

    @Test
    fun correctionsRevalidateAndRetainOriginSnapshot() {
        val original = draft(setOf(CalendarField.TIME)).validate(100)
        val corrected = original.edit(
            fields.copy(title = "Edited synthetic event"), emptySet(), 110,
        )
        assertEquals(ProposalState.READY, corrected.state)
        assertEquals(ProposalExplanation.USER_CORRECTED_FIELDS, corrected.explanation)
        assertEquals(origin, corrected.origin)
        assertEquals(ProposalState.NEEDS_REVIEW, original.state)
        assertEquals(ProposalState.HANDED_OFF, corrected.markHandedOff(110).state)
    }

    @Test
    fun terminalOutcomesRejectRepeatedActions() {
        val terminal = listOf(draft().dismiss(100), draft().validate(100).markHandedOff(100),
            draft().expire(200))
        for (proposal in terminal) {
            assertFailsWith<IllegalStateException> { proposal.validate(200) }
            assertFailsWith<IllegalStateException> { proposal.dismiss(200) }
            assertFailsWith<IllegalStateException> { proposal.edit(fields, emptySet(), 200) }
            assertFailsWith<IllegalStateException> { proposal.defer(200) }
            assertFailsWith<IllegalStateException> { proposal.fail(200) }
            assertEquals(proposal.state, proposal.expire(300).state)
        }
    }

    @Test
    fun expiryBlocksHandoffAtExactDeadline() {
        val ready = draft().validate(199)
        assertEquals(ProposalState.READY, ready.expire(199).state)
        assertEquals(ProposalState.EXPIRED, ready.expire(200).state)
        assertFailsWith<IllegalStateException> { ready.markHandedOff(200) }
        assertFailsWith<IllegalArgumentException> { ready.validate(99) }
        assertFailsWith<IllegalArgumentException> {
            CalendarProposal.draft("proposal", origin, fields, 100, 100)
        }
    }

    @Test
    fun failuresAndDeferralsCanRetryWithoutRawContent() {
        assertEquals(ProposalState.READY, draft().fail(100).validate(110).state)
        assertEquals(ProposalState.READY, draft().defer(100).validate(110).state)
        assertEquals(ProposalState.EXPIRED, draft().defer(100).expire(200).state)
    }

    @Test
    fun ambiguityIsNotAliasedAndStringOutputsAreRedacted() {
        val ambiguity = mutableSetOf(CalendarField.TIME)
        val proposal = draft(ambiguity)
        ambiguity.clear()
        assertEquals(setOf(CalendarField.TIME), proposal.ambiguousFields)
        val messages = listOf(proposal.toString(), fields.toString(), origin.toString())
        assertTrue(messages.all { "REDACTED" in it })
        assertFalse(messages.any { "Synthetic event" in it || "Synthetic location" in it })
    }

    @Test
    fun calendarValuesRejectInvalidDatesAndTimes() {
        assertEquals(29, CalendarDate(2024, 2, 29).day)
        assertEquals(29, CalendarDate(2000, 2, 29).day)
        for (date in listOf(Triple(2026, 2, 29), Triple(1900, 2, 29), Triple(2026, 4, 31),
            Triple(2026, 0, 1), Triple(0, 1, 1))) {
            assertFailsWith<IllegalArgumentException> {
                CalendarDate(date.first, date.second, date.third)
            }
        }
        assertFailsWith<IllegalArgumentException> { CalendarTime(24, 0) }
        assertFailsWith<IllegalArgumentException> { CalendarTime(0, 60) }
    }
}
