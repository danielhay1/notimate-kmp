package com.hayduck.notemate.domain.results

import com.hayduck.notemate.domain.activity.ActivityRecord
import com.hayduck.notemate.domain.proposal.CalendarProposal
import com.hayduck.notemate.domain.proposal.ProposalState

/** A revision guards commands against stale callers; timestamps never precede creation. */
data class StoredCalendarProposal(
    val proposal: CalendarProposal,
    val revision: Long,
    val updatedAtEpochMilliseconds: Long,
) {
    init {
        require(revision >= 0) { "Proposal revision is invalid." }
        require(updatedAtEpochMilliseconds >= proposal.createdAtEpochMilliseconds) {
            "Update precedes creation."
        }
        require(proposal.state != ProposalState.EXPIRED ||
            updatedAtEpochMilliseconds >= requireNotNull(proposal.expiresAtEpochMilliseconds)) {
            "Expiry precedes its deadline."
        }
        require(proposal.state == ProposalState.EXPIRED ||
            proposal.expiresAtEpochMilliseconds?.let { updatedAtEpochMilliseconds < it } != false) {
            "Non-expiry update reaches the expiry deadline."
        }
    }
}

/** An owned snapshot; identifiers remain unique and lists cannot mutate repository state. */
class LocalResults(proposals: List<StoredCalendarProposal>, activity: List<ActivityRecord>) {
    private val storedProposals = proposals.toList()
    private val storedActivity = activity.toList()
    val proposals: List<StoredCalendarProposal> get() = storedProposals.toList()
    val activity: List<ActivityRecord> get() = storedActivity.toList()

    init {
        require(storedProposals.map { it.proposal.id }.distinct().size == storedProposals.size) {
            "Proposal identifiers must be unique."
        }
        require(storedActivity.map { it.id }.distinct().size == storedActivity.size) {
            "Activity identifiers must be unique."
        }
    }

    override fun toString(): String = "LocalResults(content=[REDACTED])"
}
