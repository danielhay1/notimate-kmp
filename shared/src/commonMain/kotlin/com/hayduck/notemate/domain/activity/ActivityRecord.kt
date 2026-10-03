package com.hayduck.notemate.domain.activity

enum class ActivityEvent {
    CALENDAR_PROPOSAL_CREATED, PROPOSAL_DISMISSED, OPENED_IN_CALENDAR,
    PROPOSAL_DEFERRED, PROPOSAL_FAILED, PROPOSAL_EXPIRED, PROPOSAL_VALIDATED, PROPOSAL_EDITED,
    NOTIFICATION_IGNORED, PROFILE_ACTIVATED, NOTIFICATION_ACCESS_LOST,
}

/**
 * Local-only audit metadata with no title, body, location, prompt, or free-text message.
 * Identifiers must reference local records, never encode notification content.
 */
data class ActivityRecord(
    val id: String,
    val event: ActivityEvent,
    val occurredAtEpochMilliseconds: Long,
    val proposalId: String? = null,
    val profileId: String? = null,
    val automationId: String? = null,
    val applicationId: String? = null,
) {
    init {
        require(id.isNotBlank()) { "Activity identifier must not be blank." }
        require(listOfNotNull(proposalId, profileId, automationId, applicationId)
            .all { it.isNotBlank() }) { "Activity references must not be blank." }
    }

    override fun toString(): String = "ActivityRecord(event=$event, references=[REDACTED])"
}
