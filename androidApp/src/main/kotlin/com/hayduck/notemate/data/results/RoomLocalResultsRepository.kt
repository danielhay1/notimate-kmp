package com.hayduck.notemate.data.results

import androidx.room.withTransaction
import com.hayduck.notemate.domain.activity.ActivityEvent
import com.hayduck.notemate.domain.activity.ActivityRecord
import com.hayduck.notemate.domain.proposal.CalendarProposal
import com.hayduck.notemate.domain.results.LocalResults
import com.hayduck.notemate.domain.results.LocalResultsRepository
import com.hayduck.notemate.domain.results.ProposalChange
import com.hayduck.notemate.domain.results.StoredCalendarProposal
import com.hayduck.notemate.domain.results.activityEvent
import com.hayduck.notemate.domain.results.activityRecord
import com.hayduck.notemate.domain.results.applyChange
import com.hayduck.notemate.domain.results.asNewStoredProposal
import com.hayduck.notemate.domain.results.requireStandaloneEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class RoomLocalResultsRepository(
    private val database: LocalResultsDatabase,
) : LocalResultsRepository {
    private val dao = database.resultsDao()

    override val results: Flow<LocalResults> = flow {
        get()
        emitAll(dao.observeSnapshots().map { it.toDomain() })
    }

    override suspend fun get(): LocalResults = database.withTransaction { readOrInitialize() }

    override suspend fun createProposal(
        proposal: CalendarProposal,
        activityId: String,
    ): StoredCalendarProposal = database.withTransaction {
        readOrInitialize()
        val stored = proposal.asNewStoredProposal()
        dao.insertProposal(stored.toEntity())
        dao.insertActivity(proposal.activityRecord(
            activityId, ActivityEvent.CALENDAR_PROPOSAL_CREATED,
            proposal.createdAtEpochMilliseconds,
        ).toEntity())
        stored
    }

    override suspend fun changeProposal(
        proposalId: String,
        expectedRevision: Long,
        change: ProposalChange,
        activityId: String,
        nowEpochMilliseconds: Long,
    ): StoredCalendarProposal = database.withTransaction {
        val current = readOrInitialize().requireProposal(proposalId)
        val next = current.applyChange(expectedRevision, change, nowEpochMilliseconds)
        if (next !== current) {
            check(dao.updateProposal(next.toEntity()) == 1) { "Proposal is missing." }
            dao.insertActivity(next.proposal.activityRecord(activityId, change.activityEvent,
                nowEpochMilliseconds).toEntity())
        }
        next
    }

    override suspend fun appendActivity(activity: ActivityRecord) {
        activity.requireStandaloneEvent()
        database.withTransaction {
            readOrInitialize()
            dao.insertActivity(activity.toEntity())
        }
    }

    override suspend fun clearActivity() {
        database.withTransaction {
            readOrInitialize()
            dao.clearActivity()
        }
    }

    override suspend fun deleteProposal(proposalId: String, expectedRevision: Long) {
        database.withTransaction {
            val current = readOrInitialize().requireProposal(proposalId)
            check(current.revision == expectedRevision) { "Proposal revision is stale." }
            check(dao.deleteProposal(proposalId, expectedRevision) == 1) { "Proposal is missing." }
        }
    }

    private suspend fun readOrInitialize(): LocalResults {
        val snapshots = dao.readSnapshots()
        if (snapshots.isNotEmpty()) return snapshots.toDomain()
        dao.insertScope(ResultsScopeEntity())
        return LocalResults(emptyList(), emptyList())
    }

    private fun LocalResults.requireProposal(id: String): StoredCalendarProposal =
        requireNotNull(proposals.firstOrNull { it.proposal.id == id }) { "Proposal is missing." }
}
