package com.hayduck.notemate.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import com.hayduck.notemate.designsystem.NotiMateShapes
import com.hayduck.notemate.designsystem.NotiMateSpacing
import com.hayduck.notemate.designsystem.ProcessingStatusAction
import com.hayduck.notemate.designsystem.ProcessingStatusBanner
import com.hayduck.notemate.designsystem.ProcessingStatusUiState
import com.hayduck.notemate.designsystem.ProposalCard
import com.hayduck.notemate.designsystem.ProposalCardStatus
import com.hayduck.notemate.designsystem.ProposalCardUiState
import com.hayduck.notemate.domain.activity.ActivityEvent
import com.hayduck.notemate.domain.proposal.ProposalState
import note_mate.shared.generated.resources.Res
import note_mate.shared.generated.resources.notification_testing_open
import note_mate.shared.generated.resources.today_alert_delivery_unavailable
import note_mate.shared.generated.resources.today_alert_settings
import note_mate.shared.generated.resources.today_automations
import note_mate.shared.generated.resources.today_configuration_failed
import note_mate.shared.generated.resources.today_deferred_explanation
import note_mate.shared.generated.resources.today_draft_explanation
import note_mate.shared.generated.resources.today_empty_body
import note_mate.shared.generated.resources.today_empty_title
import note_mate.shared.generated.resources.today_expired_explanation
import note_mate.shared.generated.resources.today_failed_explanation
import note_mate.shared.generated.resources.today_missing_date
import note_mate.shared.generated.resources.today_missing_time
import note_mate.shared.generated.resources.today_missing_title
import note_mate.shared.generated.resources.today_no_outcomes
import note_mate.shared.generated.resources.today_no_sources
import note_mate.shared.generated.resources.today_outcome_access_lost
import note_mate.shared.generated.resources.today_outcome_calendar
import note_mate.shared.generated.resources.today_outcome_created
import note_mate.shared.generated.resources.today_outcome_deferred
import note_mate.shared.generated.resources.today_outcome_dismissed
import note_mate.shared.generated.resources.today_outcome_edited
import note_mate.shared.generated.resources.today_outcome_expired
import note_mate.shared.generated.resources.today_outcome_failed
import note_mate.shared.generated.resources.today_outcome_ignored
import note_mate.shared.generated.resources.today_outcome_otp_copied
import note_mate.shared.generated.resources.today_outcome_profile
import note_mate.shared.generated.resources.today_outcome_validated
import note_mate.shared.generated.resources.today_pause
import note_mate.shared.generated.resources.today_pause_explanation
import note_mate.shared.generated.resources.today_permissions
import note_mate.shared.generated.resources.today_processing_unavailable
import note_mate.shared.generated.resources.today_profile
import note_mate.shared.generated.resources.today_profile_management_later
import note_mate.shared.generated.resources.today_proposal_location
import note_mate.shared.generated.resources.today_proposal_summary
import note_mate.shared.generated.resources.today_proposals
import note_mate.shared.generated.resources.today_proposals_read_only
import note_mate.shared.generated.resources.today_ready_explanation
import note_mate.shared.generated.resources.today_recent
import note_mate.shared.generated.resources.today_results_failed
import note_mate.shared.generated.resources.today_resume
import note_mate.shared.generated.resources.today_retry
import note_mate.shared.generated.resources.today_review_explanation
import note_mate.shared.generated.resources.today_sources
import note_mate.shared.generated.resources.today_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

internal fun LazyListScope.todayFeed(
    state: TodayUiState,
    onAction: (TodayAction) -> Unit,
    formatter: TodayTextFormatter,
    onOpenNotificationTesting: (() -> Unit)?,
) {
    item(key = "header") {
        Column(verticalArrangement = Arrangement.spacedBy(NotiMateSpacing.space12)) {
            TodayHeading(stringResource(Res.string.today_title))
            TodayActionButton(Res.string.today_permissions, secondary = true) {
                onAction(TodayAction.OPEN_PERMISSIONS)
            }
            if (onOpenNotificationTesting != null) {
                TodayActionButton(Res.string.notification_testing_open, secondary = true) {
                    onOpenNotificationTesting()
                }
            }
        }
    }
    item(key = "processing") { TodayProcessingStatus(state, onAction) }
    item(key = "configuration") {
        when (val configuration = state.configuration) {
            TodayConfigurationUiState.Loading -> TodayLoading()
            TodayConfigurationUiState.Failed -> TodayMessagePanel(
                Res.string.today_configuration_failed, Res.string.today_retry,
            ) { onAction(TodayAction.RETRY_CONFIGURATION) }
            is TodayConfigurationUiState.Ready -> ActiveProfileSummary(
                configuration, state.isSaving, onAction,
            )
        }
    }
    item(key = "alerts") {
        Column(verticalArrangement = Arrangement.spacedBy(NotiMateSpacing.space8)) {
            Text(stringResource(alertPermissionLabel(state.alerts)))
            Text(
                stringResource(Res.string.today_alert_delivery_unavailable),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (state.alerts != ProposalAlertPermission.GRANTED) {
                TodayActionButton(Res.string.today_alert_settings, secondary = true) {
                    onAction(TodayAction.OPEN_PERMISSIONS)
                }
            }
        }
    }
    item(key = "proposals-heading") {
        Column(verticalArrangement = Arrangement.spacedBy(NotiMateSpacing.space8)) {
            TodayHeading(stringResource(Res.string.today_proposals))
            Text(
                stringResource(Res.string.today_proposals_read_only),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    when (val results = state.results) {
        TodayResultsUiState.Loading -> item(key = "results-loading") { TodayLoading() }
        TodayResultsUiState.Failed -> item(key = "results-failed") {
            TodayMessagePanel(Res.string.today_results_failed, Res.string.today_retry) {
                onAction(TodayAction.RETRY_RESULTS)
            }
        }
        is TodayResultsUiState.Ready -> {
            if (results.proposals.isEmpty()) {
                item(key = "empty") { TodayEmptyState() }
            }
            items(results.proposals, key = { "proposal:${it.id}" }) { proposal ->
                SavedProposalCard(proposal, formatter)
            }
            item(key = "recent-heading") {
                TodayHeading(stringResource(Res.string.today_recent))
            }
            if (results.recentOutcomes.isEmpty()) {
                item(key = "no-outcomes") {
                    Text(stringResource(Res.string.today_no_outcomes))
                }
            }
            items(results.recentOutcomes, key = { "outcome:${it.id}" }) {
                ActivityRow(it, formatter)
            }
        }
    }
    item(key = "privacy") { PrivacyIndicator() }
}

@Composable
private fun TodayProcessingStatus(state: TodayUiState, onAction: (TodayAction) -> Unit) {
    val status = when (state.processing) {
        TodayProcessingState.CHECKING, TodayProcessingState.CONFIGURATION_UNAVAILABLE -> return
        TodayProcessingState.PERMISSION_REQUIRED -> ProcessingStatusUiState.PermissionRequired
        TodayProcessingState.PAUSED -> ProcessingStatusUiState.Paused
        TodayProcessingState.PROCESSOR_UNAVAILABLE -> ProcessingStatusUiState.Unavailable(
            stringResource(Res.string.today_processing_unavailable),
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(NotiMateSpacing.space12)) {
        ProcessingStatusBanner(
            status,
            onAction = { action ->
                when (action) {
                    ProcessingStatusAction.RESUME -> onAction(TodayAction.RESUME)
                    ProcessingStatusAction.OPEN_NOTIFICATION_ACCESS_SETTINGS ->
                        onAction(TodayAction.OPEN_PERMISSIONS)
                    else -> Unit
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.processing != TodayProcessingState.PROCESSOR_UNAVAILABLE) {
            Text(
                stringResource(Res.string.today_processing_unavailable),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun ActiveProfileSummary(
    state: TodayConfigurationUiState.Ready,
    isSaving: Boolean,
    onAction: (TodayAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(NotiMateSpacing.space12)) {
        Surface(
            shape = NotiMateShapes.compactPanel,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Text(
                stringResource(Res.string.today_profile, state.profileName),
                Modifier.fillMaxWidth().padding(NotiMateSpacing.space12),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Text(stringResource(Res.string.today_sources, state.monitoredSourceCount))
        Text(stringResource(Res.string.today_automations, state.enabledAutomationCount))
        if (state.monitoredSourceCount == 0) {
            Text(stringResource(Res.string.today_no_sources))
        }
        Text(
            stringResource(Res.string.today_profile_management_later),
            style = MaterialTheme.typography.bodyMedium,
        )
        TodayActionButton(
            if (state.isPaused) Res.string.today_resume else Res.string.today_pause,
            enabled = !isSaving,
            secondary = true,
        ) { onAction(if (state.isPaused) TodayAction.RESUME else TodayAction.PAUSE) }
        Text(
            stringResource(Res.string.today_pause_explanation),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun TodayEmptyState() {
    Column(verticalArrangement = Arrangement.spacedBy(NotiMateSpacing.space8)) {
        Text(
            stringResource(Res.string.today_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(stringResource(Res.string.today_empty_body))
    }
}

@Composable
private fun SavedProposalCard(proposal: TodayProposalUiState, formatter: TodayTextFormatter) {
    val status = when (proposal.state) {
        ProposalState.DRAFT -> ProposalCardStatus.DRAFT
        ProposalState.NEEDS_REVIEW -> ProposalCardStatus.NEEDS_REVIEW
        ProposalState.READY -> ProposalCardStatus.READY
        ProposalState.DEFERRED -> ProposalCardStatus.DEFERRED
        ProposalState.FAILED -> ProposalCardStatus.FAILED
        ProposalState.EXPIRED -> ProposalCardStatus.EXPIRED
        ProposalState.DISMISSED, ProposalState.HANDED_OFF -> return
    }
    val date = proposal.date?.let(formatter::date)
        ?: stringResource(Res.string.today_missing_date)
    val time = proposal.time?.let(formatter::time)
        ?: stringResource(Res.string.today_missing_time)
    val explanation = when (status) {
        ProposalCardStatus.DRAFT -> Res.string.today_draft_explanation
        ProposalCardStatus.NEEDS_REVIEW -> Res.string.today_review_explanation
        ProposalCardStatus.READY -> Res.string.today_ready_explanation
        ProposalCardStatus.DEFERRED -> Res.string.today_deferred_explanation
        ProposalCardStatus.FAILED -> Res.string.today_failed_explanation
        ProposalCardStatus.EXPIRED -> Res.string.today_expired_explanation
    }
    Column(verticalArrangement = Arrangement.spacedBy(NotiMateSpacing.space8)) {
        ProposalCard(
            state = ProposalCardUiState(
                title = proposal.title?.takeIf { it.isNotBlank() }
                    ?: stringResource(Res.string.today_missing_title),
                summary = stringResource(Res.string.today_proposal_summary, date, time),
                sourceName = proposal.sourceName,
                profileName = proposal.profileName,
                automationName = proposal.automationName,
                status = status,
                explanation = stringResource(explanation),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        proposal.location?.let {
            Text(stringResource(Res.string.today_proposal_location, it))
        }
    }
}

@Composable
private fun ActivityRow(state: RecentOutcomeUiState, formatter: TodayTextFormatter) {
    Column(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(NotiMateSpacing.space4),
    ) {
        Text(
            stringResource(activityLabel(state.event)),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            formatter.timestamp(state.occurredAtEpochMilliseconds),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private fun activityLabel(event: ActivityEvent): StringResource = when (event) {
    ActivityEvent.CALENDAR_PROPOSAL_CREATED -> Res.string.today_outcome_created
    ActivityEvent.PROPOSAL_DISMISSED -> Res.string.today_outcome_dismissed
    ActivityEvent.OPENED_IN_CALENDAR -> Res.string.today_outcome_calendar
    ActivityEvent.PROPOSAL_DEFERRED -> Res.string.today_outcome_deferred
    ActivityEvent.PROPOSAL_FAILED -> Res.string.today_outcome_failed
    ActivityEvent.PROPOSAL_EXPIRED -> Res.string.today_outcome_expired
    ActivityEvent.PROPOSAL_VALIDATED -> Res.string.today_outcome_validated
    ActivityEvent.PROPOSAL_EDITED -> Res.string.today_outcome_edited
    ActivityEvent.OTP_COPIED_TO_CLIPBOARD -> Res.string.today_outcome_otp_copied
    ActivityEvent.NOTIFICATION_IGNORED -> Res.string.today_outcome_ignored
    ActivityEvent.PROFILE_ACTIVATED -> Res.string.today_outcome_profile
    ActivityEvent.NOTIFICATION_ACCESS_LOST -> Res.string.today_outcome_access_lost
}
