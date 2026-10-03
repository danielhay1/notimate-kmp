package com.hayduck.notemate.domain.proposal

enum class ProposalState {
    DRAFT, NEEDS_REVIEW, READY, DISMISSED, HANDED_OFF, DEFERRED, FAILED, EXPIRED,
}

enum class ProposalExplanation { POSSIBLE_CALENDAR_EVENT, USER_CORRECTED_FIELDS }

/**
 * Immutable local suggestion with guarded transitions and no external side effects.
 * Callers supply the clock and own atomic persistence of transitions.
 * Structured content stays local.
 * Commands reject terminal or expired proposals; clocks before creation are invalid.
 */
class CalendarProposal private constructor(
    val id: String,
    val origin: ProposalOrigin,
    val fields: CalendarFields,
    ambiguousFields: Set<CalendarField>,
    val explanation: ProposalExplanation,
    val state: ProposalState,
    val createdAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long?,
) {
    private val ambiguity = ambiguousFields.toSet()
    val ambiguousFields: Set<CalendarField> get() = ambiguity.toSet()
    val issues: Set<ProposalIssue> get() = fields.validationIssues(ambiguity)

    /** Validates a draft or retries a failed/deferred proposal without external work. */
    fun validate(nowEpochMilliseconds: Long): CalendarProposal {
        checkActive(nowEpochMilliseconds)
        return transition(if (issues.isEmpty()) ProposalState.READY else ProposalState.NEEDS_REVIEW)
    }

    /** Revalidates user-edited structured fields; uncertainty must be resolved explicitly. */
    fun edit(
        fields: CalendarFields,
        ambiguousFields: Set<CalendarField>,
        nowEpochMilliseconds: Long,
    ): CalendarProposal {
        checkActive(nowEpochMilliseconds)
        return CalendarProposal(id, origin, fields, ambiguousFields,
            ProposalExplanation.USER_CORRECTED_FIELDS, ProposalState.DRAFT,
            createdAtEpochMilliseconds, expiresAtEpochMilliseconds).validate(nowEpochMilliseconds)
    }

    fun dismiss(nowEpochMilliseconds: Long): CalendarProposal {
        checkActive(nowEpochMilliseconds)
        return transition(ProposalState.DISMISSED)
    }

    /** Invoke only after explicit user confirmation and successful opening of Calendar's UI. */
    fun markHandedOff(nowEpochMilliseconds: Long): CalendarProposal {
        checkActive(nowEpochMilliseconds)
        check(state == ProposalState.READY && issues.isEmpty()) { "Proposal is not ready." }
        return transition(ProposalState.HANDED_OFF)
    }

    fun defer(nowEpochMilliseconds: Long): CalendarProposal {
        checkActive(nowEpochMilliseconds)
        return transition(ProposalState.DEFERRED)
    }

    fun fail(nowEpochMilliseconds: Long): CalendarProposal {
        checkActive(nowEpochMilliseconds)
        return transition(ProposalState.FAILED)
    }

    /** Terminal outcomes remain unchanged; expiry is reached at the exact deadline. */
    fun expire(nowEpochMilliseconds: Long): CalendarProposal {
        require(nowEpochMilliseconds >= createdAtEpochMilliseconds) { "Clock precedes creation." }
        return if (!isTerminal && isPastExpiry(nowEpochMilliseconds)) {
            transition(ProposalState.EXPIRED)
        } else this
    }

    private val isTerminal: Boolean get() = state in setOf(
        ProposalState.DISMISSED, ProposalState.HANDED_OFF, ProposalState.EXPIRED,
    )

    private fun isPastExpiry(now: Long): Boolean = expiresAtEpochMilliseconds?.let { now >= it }
        ?: false

    private fun checkActive(now: Long) {
        require(now >= createdAtEpochMilliseconds) { "Clock precedes creation." }
        check(!isTerminal && !isPastExpiry(now)) { "Proposal is no longer actionable." }
    }

    private fun transition(state: ProposalState): CalendarProposal = CalendarProposal(
        id, origin, fields, ambiguity, explanation, state,
        createdAtEpochMilliseconds, expiresAtEpochMilliseconds,
    )

    override fun toString(): String = "CalendarProposal(state=$state, content=[REDACTED])"

    companion object {
        /** Restores a structured local record without replaying commands or changing its state. */
        fun restore(
            id: String,
            origin: ProposalOrigin,
            fields: CalendarFields,
            ambiguousFields: Set<CalendarField>,
            explanation: ProposalExplanation,
            state: ProposalState,
            createdAtEpochMilliseconds: Long,
            expiresAtEpochMilliseconds: Long?,
        ): CalendarProposal {
            val draft = draft(id, origin, fields, createdAtEpochMilliseconds,
                expiresAtEpochMilliseconds, ambiguousFields)
            require(state !in setOf(ProposalState.READY, ProposalState.HANDED_OFF) ||
                draft.issues.isEmpty()) { "Ready proposals must have valid fields." }
            require(state != ProposalState.NEEDS_REVIEW || draft.issues.isNotEmpty()) {
                "Review proposals must have unresolved issues."
            }
            require(state != ProposalState.EXPIRED || expiresAtEpochMilliseconds != null) {
                "Expired proposals must have an expiry."
            }
            require(state != ProposalState.DRAFT ||
                explanation == ProposalExplanation.POSSIBLE_CALENDAR_EVENT) {
                "Draft explanation is invalid."
            }
            return CalendarProposal(id, origin, fields, ambiguousFields, explanation, state,
                createdAtEpochMilliseconds, expiresAtEpochMilliseconds)
        }

        /** Creates an unvalidated draft; expiry policy belongs to the caller. */
        fun draft(
            id: String,
            origin: ProposalOrigin,
            fields: CalendarFields,
            createdAtEpochMilliseconds: Long,
            expiresAtEpochMilliseconds: Long? = null,
            ambiguousFields: Set<CalendarField> = emptySet(),
        ): CalendarProposal {
            require(id.isNotBlank()) { "Proposal identifier must not be blank." }
            require(expiresAtEpochMilliseconds == null ||
                expiresAtEpochMilliseconds > createdAtEpochMilliseconds) {
                "Expiry must follow creation."
            }
            return CalendarProposal(id, origin, fields, ambiguousFields,
                ProposalExplanation.POSSIBLE_CALENDAR_EVENT, ProposalState.DRAFT,
                createdAtEpochMilliseconds, expiresAtEpochMilliseconds)
        }
    }
}
