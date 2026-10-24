package com.hayduck.notemate.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import note_mate.shared.generated.resources.Res
import note_mate.shared.generated.resources.ds_check_circle
import note_mate.shared.generated.resources.ds_error
import note_mate.shared.generated.resources.ds_info
import note_mate.shared.generated.resources.ds_pause_circle
import note_mate.shared.generated.resources.ds_review
import note_mate.shared.generated.resources.proposal_automation
import note_mate.shared.generated.resources.proposal_deferred
import note_mate.shared.generated.resources.proposal_dismiss
import note_mate.shared.generated.resources.proposal_draft
import note_mate.shared.generated.resources.proposal_expired
import note_mate.shared.generated.resources.proposal_failed
import note_mate.shared.generated.resources.proposal_needs_review
import note_mate.shared.generated.resources.proposal_profile
import note_mate.shared.generated.resources.proposal_ready
import note_mate.shared.generated.resources.proposal_review
import note_mate.shared.generated.resources.proposal_source
import note_mate.shared.generated.resources.proposal_type_calendar
import note_mate.shared.generated.resources.try_again
import org.jetbrains.compose.resources.stringResource

/**
 * Local structured summary; caller-owned actions require domain validation.
 * A null action handler renders an informational card.
 */
@Composable
fun ProposalCard(
    state: ProposalCardUiState,
    onAction: ((ProposalCardAction) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val colors = NotiMateTheme.semanticColors
    val status = state.status
    val container = when (status) {
        ProposalCardStatus.DRAFT -> MaterialTheme.colorScheme.surfaceVariant
        ProposalCardStatus.NEEDS_REVIEW -> colors.review
        ProposalCardStatus.READY -> MaterialTheme.colorScheme.primaryContainer
        ProposalCardStatus.DEFERRED -> colors.paused
        ProposalCardStatus.FAILED -> colors.failure
        ProposalCardStatus.EXPIRED -> colors.degraded
    }
    val content = when (status) {
        ProposalCardStatus.DRAFT -> MaterialTheme.colorScheme.onSurfaceVariant
        ProposalCardStatus.NEEDS_REVIEW -> colors.onReview
        ProposalCardStatus.READY -> MaterialTheme.colorScheme.onPrimaryContainer
        ProposalCardStatus.DEFERRED -> colors.onPaused
        ProposalCardStatus.FAILED -> colors.onFailure
        ProposalCardStatus.EXPIRED -> colors.onDegraded
    }
    val label = when (status) {
        ProposalCardStatus.DRAFT -> Res.string.proposal_draft
        ProposalCardStatus.NEEDS_REVIEW -> Res.string.proposal_needs_review
        ProposalCardStatus.READY -> Res.string.proposal_ready
        ProposalCardStatus.DEFERRED -> Res.string.proposal_deferred
        ProposalCardStatus.FAILED -> Res.string.proposal_failed
        ProposalCardStatus.EXPIRED -> Res.string.proposal_expired
    }
    val icon = when (status) {
        ProposalCardStatus.DRAFT -> Res.drawable.ds_info
        ProposalCardStatus.NEEDS_REVIEW -> Res.drawable.ds_review
        ProposalCardStatus.READY -> Res.drawable.ds_check_circle
        ProposalCardStatus.DEFERRED -> Res.drawable.ds_pause_circle
        ProposalCardStatus.FAILED -> Res.drawable.ds_error
        ProposalCardStatus.EXPIRED -> Res.drawable.ds_info
    }
    Card(
        modifier = modifier.semantics { isTraversalGroup = true },
        shape = NotiMateShapes.card,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(NotiMateSpacing.space16),
            verticalArrangement = Arrangement.spacedBy(NotiMateSpacing.space12),
        ) {
            Surface(
                shape = NotiMateShapes.compactPanel,
                color = container,
                contentColor = content,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(NotiMateSpacing.space12),
                    verticalArrangement = Arrangement.spacedBy(NotiMateSpacing.space8),
                ) {
                    StatusHeading(stringResource(label), icon)
                    Text(state.explanation, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Column(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(NotiMateSpacing.space8),
            ) {
                Text(
                    stringResource(Res.string.proposal_type_calendar),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(state.title, style = MaterialTheme.typography.titleMedium)
                Text(state.summary, style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(Res.string.proposal_source, state.sourceName),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    stringResource(Res.string.proposal_profile, state.profileName),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    stringResource(Res.string.proposal_automation, state.automationName),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (onAction != null && status != ProposalCardStatus.EXPIRED) {
                Button(
                    onClick = {
                        onAction(if (status == ProposalCardStatus.FAILED) {
                            ProposalCardAction.RETRY
                        } else {
                            ProposalCardAction.REVIEW
                        })
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = NotiMateSpacing.space48),
                ) {
                    Text(stringResource(if (status == ProposalCardStatus.FAILED) {
                        Res.string.try_again
                    } else {
                        Res.string.proposal_review
                    }))
                }
                TextButton(
                    onClick = { onAction(ProposalCardAction.DISMISS) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = NotiMateSpacing.space48),
                ) {
                    Text(stringResource(Res.string.proposal_dismiss))
                }
            }
        }
    }
}
