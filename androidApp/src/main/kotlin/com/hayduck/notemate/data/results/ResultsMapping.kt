package com.hayduck.notemate.data.results

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
import com.hayduck.notemate.domain.results.LocalResults
import com.hayduck.notemate.domain.results.StoredCalendarProposal

internal fun List<ResultsSnapshot>.toDomain(): LocalResults {
    check(size == 1 && single().scope.id == 0) { "Results scope is invalid." }
    val snapshot = single()
    return LocalResults(
        snapshot.proposals.map { it.toDomain() }.sortedWith(
            compareBy<StoredCalendarProposal> { it.proposal.createdAtEpochMilliseconds }
                .thenBy { it.proposal.id },
        ),
        snapshot.activity.map { it.toDomain() }.sortedWith(
            compareBy<ActivityRecord> { it.occurredAtEpochMilliseconds }.thenBy { it.id },
        ),
    )
}

internal fun StoredProposalEntity.toDomain(): StoredCalendarProposal {
    require(ownerId == 0) { "Proposal owner is invalid." }
    val dateParts = listOf(dateYear, dateMonth, dateDay)
    require(dateParts.all { it == null } || dateParts.all { it != null }) {
        "Stored date is incomplete."
    }
    val timeParts = listOf(timeHour, timeMinute)
    require(timeParts.all { it == null } || timeParts.all { it != null }) {
        "Stored time is incomplete."
    }
    val ambiguity = if (ambiguousFields.isEmpty()) emptyList() else ambiguousFields.split(',')
    require(ambiguity.distinct().size == ambiguity.size) { "Stored ambiguity is duplicated." }
    val proposal = CalendarProposal.restore(
        id = id,
        origin = ProposalOrigin(profileId, profileName, automationId, automationName,
            applicationId, applicationName),
        fields = CalendarFields(
            title,
            dateYear?.let { CalendarDate(it, requireNotNull(dateMonth), requireNotNull(dateDay)) },
            timeHour?.let { CalendarTime(it, requireNotNull(timeMinute)) },
            location,
        ),
        ambiguousFields = ambiguity.map { storedEnum<CalendarField>(it) }.toSet(),
        explanation = storedEnum<ProposalExplanation>(explanation),
        state = storedEnum<ProposalState>(state),
        createdAtEpochMilliseconds = createdAtEpochMilliseconds,
        expiresAtEpochMilliseconds = expiresAtEpochMilliseconds,
    )
    return StoredCalendarProposal(proposal, revision, updatedAtEpochMilliseconds)
}

internal fun StoredCalendarProposal.toEntity(): StoredProposalEntity = proposal.let {
    StoredProposalEntity(
        id = it.id,
        profileId = it.origin.profileId,
        profileName = it.origin.profileName,
        automationId = it.origin.automationId,
        automationName = it.origin.automationName,
        applicationId = it.origin.applicationId,
        applicationName = it.origin.applicationName,
        title = it.fields.title,
        dateYear = it.fields.date?.year,
        dateMonth = it.fields.date?.month,
        dateDay = it.fields.date?.day,
        timeHour = it.fields.time?.hour,
        timeMinute = it.fields.time?.minute,
        location = it.fields.location,
        ambiguousFields = it.ambiguousFields.sortedBy { field -> field.ordinal }
            .joinToString(",") { field -> field.name },
        explanation = it.explanation.name,
        state = it.state.name,
        createdAtEpochMilliseconds = it.createdAtEpochMilliseconds,
        expiresAtEpochMilliseconds = it.expiresAtEpochMilliseconds,
        revision = revision,
        updatedAtEpochMilliseconds = updatedAtEpochMilliseconds,
    )
}

internal fun StoredActivityEntity.toDomain(): ActivityRecord {
    require(ownerId == 0) { "Activity owner is invalid." }
    return ActivityRecord(id, storedEnum<ActivityEvent>(event), occurredAtEpochMilliseconds,
        proposalId, profileId, automationId, applicationId)
}

internal fun ActivityRecord.toEntity(): StoredActivityEntity = StoredActivityEntity(
    id = id,
    event = event.name,
    occurredAtEpochMilliseconds = occurredAtEpochMilliseconds,
    proposalId = proposalId,
    profileId = profileId,
    automationId = automationId,
    applicationId = applicationId,
)

private inline fun <reified T : Enum<T>> storedEnum(value: String): T =
    requireNotNull(enumValues<T>().firstOrNull { it.name == value }) { "Stored type is invalid." }
