package com.hayduck.notemate.domain.results

import com.hayduck.notemate.domain.activity.ActivityEvent
import com.hayduck.notemate.domain.activity.ActivityRecord
import com.hayduck.notemate.domain.proposal.CalendarField
import com.hayduck.notemate.domain.proposal.CalendarFields
import com.hayduck.notemate.domain.proposal.CalendarProposal
import com.hayduck.notemate.domain.proposal.ProposalState

/** Commands preserve identity, origin, creation, and caller-supplied expiry. */
sealed interface ProposalChange {
    data object Validate : ProposalChange
    class Edit(val fields: CalendarFields, ambiguousFields: Set<CalendarField>) : ProposalChange {
        private val ambiguity = ambiguousFields.toSet()
        val ambiguousFields: Set<CalendarField> get() = ambiguity.toSet()
        override fun toString(): String = "Edit(content=[REDACTED])"
    }
    data object Dismiss : ProposalChange
    data object MarkHandedOff : ProposalChange
    data object Defer : ProposalChange
    data object Fail : ProposalChange
    data object Expire : ProposalChange
}

/** Validates the initial durable state without inventing an expiry or changing model output. */
fun CalendarProposal.asNewStoredProposal(): StoredCalendarProposal {
    require(state in setOf(ProposalState.DRAFT, ProposalState.READY, ProposalState.NEEDS_REVIEW)) {
        "New proposal state is invalid."
    }
    return StoredCalendarProposal(this, 0, createdAtEpochMilliseconds)
}

/** Pure lifecycle policy; repositories apply it to the latest row inside the write transaction. */
fun StoredCalendarProposal.applyChange(
    expectedRevision: Long,
    change: ProposalChange,
    nowEpochMilliseconds: Long,
): StoredCalendarProposal {
    check(revision == expectedRevision) { "Proposal revision is stale." }
    require(nowEpochMilliseconds >= updatedAtEpochMilliseconds) { "Clock precedes last update." }
    val next = when (change) {
        ProposalChange.Validate -> proposal.validate(nowEpochMilliseconds)
        is ProposalChange.Edit -> proposal.edit(change.fields, change.ambiguousFields,
            nowEpochMilliseconds)
        ProposalChange.Dismiss -> proposal.dismiss(nowEpochMilliseconds)
        ProposalChange.MarkHandedOff -> proposal.markHandedOff(nowEpochMilliseconds)
        ProposalChange.Defer -> proposal.defer(nowEpochMilliseconds)
        ProposalChange.Fail -> proposal.fail(nowEpochMilliseconds)
        ProposalChange.Expire -> proposal.expire(nowEpochMilliseconds)
    }
    if (next === proposal) return this
    check(revision < Long.MAX_VALUE) { "Proposal revision is exhausted." }
    return StoredCalendarProposal(next, revision + 1, nowEpochMilliseconds)
}

/** Produces metadata only; structured event fields and origin labels are excluded. */
fun CalendarProposal.activityRecord(
    activityId: String,
    event: ActivityEvent,
    occurredAtEpochMilliseconds: Long,
): ActivityRecord = ActivityRecord(activityId, event, occurredAtEpochMilliseconds, id,
    origin.profileId, origin.automationId, origin.applicationId)

/** Typed audit event corresponding to an atomic lifecycle command. */
val ProposalChange.activityEvent: ActivityEvent
    get() = when (this) {
        ProposalChange.Validate -> ActivityEvent.PROPOSAL_VALIDATED
        is ProposalChange.Edit -> ActivityEvent.PROPOSAL_EDITED
        ProposalChange.Dismiss -> ActivityEvent.PROPOSAL_DISMISSED
        ProposalChange.MarkHandedOff -> ActivityEvent.OPENED_IN_CALENDAR
        ProposalChange.Defer -> ActivityEvent.PROPOSAL_DEFERRED
        ProposalChange.Fail -> ActivityEvent.PROPOSAL_FAILED
        ProposalChange.Expire -> ActivityEvent.PROPOSAL_EXPIRED
    }

/** Standalone events cannot impersonate a proposal lifecycle mutation. */
fun ActivityRecord.requireStandaloneEvent() {
    require(event in setOf(ActivityEvent.NOTIFICATION_IGNORED, ActivityEvent.PROFILE_ACTIVATED,
        ActivityEvent.NOTIFICATION_ACCESS_LOST) && proposalId == null) {
        "Proposal activity requires an atomic lifecycle command."
    }
}
