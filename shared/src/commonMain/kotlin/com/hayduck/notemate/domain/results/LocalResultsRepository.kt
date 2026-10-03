package com.hayduck.notemate.domain.results

import com.hayduck.notemate.domain.activity.ActivityRecord
import com.hayduck.notemate.domain.proposal.CalendarProposal
import kotlinx.coroutines.flow.Flow

/**
 * Local source of truth for structured proposals and content-free activity metadata.
 * Operations are main-safe; storage, validation, and cancellation failures propagate without reset.
 * Mutations and their activity records commit atomically. Cancellation can arrive after commit;
 * callers must reload before retrying. Records are retained until explicitly deleted.
 */
interface LocalResultsRepository {
    /** Complete committed snapshots; invalid stored records fail observation rather than disappear. */
    val results: Flow<LocalResults>

    suspend fun get(): LocalResults

    /** Inserts a new draft, ready, or review-needed proposal with a generated creation event. */
    suspend fun createProposal(proposal: CalendarProposal, activityId: String): StoredCalendarProposal

    /**
     * Applies a command to the current record; stale revisions and backwards clocks are rejected.
     * An expiry command that changes nothing creates no activity or revision.
     * [ProposalChange.MarkHandedOff] requires prior user confirmation and a successful Calendar UI
     * handoff; this repository never performs external writes or opens provider UI.
     */
    suspend fun changeProposal(
        proposalId: String,
        expectedRevision: Long,
        change: ProposalChange,
        activityId: String,
        nowEpochMilliseconds: Long,
    ): StoredCalendarProposal

    /** Appends a non-proposal event; proposal lifecycle events belong to the atomic commands. */
    suspend fun appendActivity(activity: ActivityRecord)

    /** Deletes only activity; proposal state, revisions, and configuration remain intact. */
    suspend fun clearActivity()

    /** Deletes the selected revision; existing activity retains its metadata references. */
    suspend fun deleteProposal(proposalId: String, expectedRevision: Long)
}
