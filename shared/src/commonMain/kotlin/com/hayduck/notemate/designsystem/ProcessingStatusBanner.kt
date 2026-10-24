package com.hayduck.notemate.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import note_mate.shared.generated.resources.Res
import note_mate.shared.generated.resources.ds_check_circle
import note_mate.shared.generated.resources.ds_error
import note_mate.shared.generated.resources.ds_info
import note_mate.shared.generated.resources.ds_pause_circle
import note_mate.shared.generated.resources.processing_active
import note_mate.shared.generated.resources.processing_degraded
import note_mate.shared.generated.resources.processing_paused
import note_mate.shared.generated.resources.processing_permission_body
import note_mate.shared.generated.resources.processing_permission_required
import note_mate.shared.generated.resources.processing_profile
import note_mate.shared.generated.resources.processing_settings
import note_mate.shared.generated.resources.processing_unavailable
import note_mate.shared.generated.resources.resume
import note_mate.shared.generated.resources.review_later
import note_mate.shared.generated.resources.try_again
import note_mate.shared.generated.resources.use_basic_processing
import org.jetbrains.compose.resources.stringResource

/** Displays caller-owned processing state and emits recovery intent without executing it. */
@Composable
fun ProcessingStatusBanner(
    state: ProcessingStatusUiState,
    onAction: (ProcessingStatusAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = NotiMateTheme.semanticColors
    val container = when (state) {
        is ProcessingStatusUiState.Active -> MaterialTheme.colorScheme.primaryContainer
        ProcessingStatusUiState.Paused -> colors.paused
        ProcessingStatusUiState.PermissionRequired -> colors.review
        is ProcessingStatusUiState.Degraded -> colors.degraded
        is ProcessingStatusUiState.Unavailable -> colors.failure
    }
    val content = when (state) {
        is ProcessingStatusUiState.Active -> MaterialTheme.colorScheme.onPrimaryContainer
        ProcessingStatusUiState.Paused -> colors.onPaused
        ProcessingStatusUiState.PermissionRequired -> colors.onReview
        is ProcessingStatusUiState.Degraded -> colors.onDegraded
        is ProcessingStatusUiState.Unavailable -> colors.onFailure
    }
    val title = when (state) {
        is ProcessingStatusUiState.Active -> Res.string.processing_active
        ProcessingStatusUiState.Paused -> Res.string.processing_paused
        ProcessingStatusUiState.PermissionRequired -> Res.string.processing_permission_required
        is ProcessingStatusUiState.Degraded -> Res.string.processing_degraded
        is ProcessingStatusUiState.Unavailable -> Res.string.processing_unavailable
    }
    val icon = when (state) {
        is ProcessingStatusUiState.Active -> Res.drawable.ds_check_circle
        ProcessingStatusUiState.Paused -> Res.drawable.ds_pause_circle
        ProcessingStatusUiState.PermissionRequired -> Res.drawable.ds_info
        is ProcessingStatusUiState.Degraded -> Res.drawable.ds_info
        is ProcessingStatusUiState.Unavailable -> Res.drawable.ds_error
    }
    val action = when (state) {
        is ProcessingStatusUiState.Active -> null
        ProcessingStatusUiState.Paused -> ProcessingStatusAction.RESUME
        ProcessingStatusUiState.PermissionRequired ->
            ProcessingStatusAction.OPEN_NOTIFICATION_ACCESS_SETTINGS
        is ProcessingStatusUiState.Degraded -> ProcessingStatusAction.USE_BASIC_PROCESSING
        is ProcessingStatusUiState.Unavailable -> when (state.recovery) {
            ProcessingRecovery.TRY_AGAIN -> ProcessingStatusAction.TRY_AGAIN
            ProcessingRecovery.REVIEW_LATER -> ProcessingStatusAction.REVIEW_LATER
            null -> null
        }
    }
    Surface(
        modifier = modifier.semantics { isTraversalGroup = true },
        shape = NotiMateShapes.card,
        color = container,
        contentColor = content,
    ) {
        Column(
            modifier = Modifier.padding(NotiMateSpacing.space16),
            verticalArrangement = Arrangement.spacedBy(NotiMateSpacing.space12),
        ) {
            StatusHeading(stringResource(title), icon)
            when (state) {
                is ProcessingStatusUiState.Active -> Text(
                    stringResource(Res.string.processing_profile, state.profileName),
                )
                ProcessingStatusUiState.Paused -> Unit
                ProcessingStatusUiState.PermissionRequired -> Text(
                    stringResource(Res.string.processing_permission_body),
                )
                is ProcessingStatusUiState.Degraded -> Text(state.explanation)
                is ProcessingStatusUiState.Unavailable -> Text(state.explanation)
            }
            if (action != null) {
                Button(
                    onClick = { onAction(action) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = NotiMateSpacing.space48),
                ) {
                    Text(
                        text = stringResource(when (action) {
                            ProcessingStatusAction.RESUME -> Res.string.resume
                            ProcessingStatusAction.OPEN_NOTIFICATION_ACCESS_SETTINGS ->
                                Res.string.processing_settings
                            ProcessingStatusAction.USE_BASIC_PROCESSING ->
                                Res.string.use_basic_processing
                            ProcessingStatusAction.TRY_AGAIN -> Res.string.try_again
                            ProcessingStatusAction.REVIEW_LATER -> Res.string.review_later
                        }),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}
