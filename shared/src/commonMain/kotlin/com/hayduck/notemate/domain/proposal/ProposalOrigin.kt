package com.hayduck.notemate.domain.proposal

/** Snapshot of origin labels; later Profile edits must not change existing proposals. */
data class ProposalOrigin(
    val profileId: String,
    val profileName: String,
    val automationId: String,
    val automationName: String,
    val applicationId: String,
    val applicationName: String,
) {
    init {
        require(listOf(profileId, profileName, automationId, automationName,
            applicationId, applicationName).all { it.isNotBlank() }) {
            "Proposal origin fields must not be blank."
        }
    }

    override fun toString(): String = "ProposalOrigin([REDACTED])"
}
