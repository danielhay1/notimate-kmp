package com.hayduck.notemate.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.hayduck.notemate.designsystem.NotiMateSpacing
import note_mate.shared.generated.resources.Res
import note_mate.shared.generated.resources.today_access_body
import note_mate.shared.generated.resources.today_access_checking
import note_mate.shared.generated.resources.today_access_granted
import note_mate.shared.generated.resources.today_access_not_granted
import note_mate.shared.generated.resources.today_access_revoked
import note_mate.shared.generated.resources.today_access_title
import note_mate.shared.generated.resources.today_alert_delivery_unavailable
import note_mate.shared.generated.resources.today_alert_settings
import note_mate.shared.generated.resources.today_alerts_app_disabled
import note_mate.shared.generated.resources.today_alerts_body
import note_mate.shared.generated.resources.today_alerts_channel_disabled
import note_mate.shared.generated.resources.today_alerts_denied
import note_mate.shared.generated.resources.today_alerts_granted
import note_mate.shared.generated.resources.today_alerts_not_requested
import note_mate.shared.generated.resources.today_alerts_title
import note_mate.shared.generated.resources.today_back
import note_mate.shared.generated.resources.today_continue
import note_mate.shared.generated.resources.today_enable_alerts
import note_mate.shared.generated.resources.today_finish
import note_mate.shared.generated.resources.today_not_now
import note_mate.shared.generated.resources.today_open_settings
import note_mate.shared.generated.resources.today_permissions
import note_mate.shared.generated.resources.today_previous
import note_mate.shared.generated.resources.today_privacy_body
import note_mate.shared.generated.resources.today_privacy_title
import note_mate.shared.generated.resources.today_processing_unavailable
import note_mate.shared.generated.resources.today_start
import note_mate.shared.generated.resources.today_welcome_body
import note_mate.shared.generated.resources.today_welcome_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PermissionOnboardingContent(state: TodayUiState, onAction: (TodayAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(NotiMateSpacing.space24)) {
        when (state.destination) {
            TodayDestination.WELCOME -> {
                PermissionEducationPanel(
                    Res.string.today_welcome_title, Res.string.today_welcome_body,
                )
                PrivacyIndicator()
                TodayActionButton(Res.string.today_start, !state.isSaving) {
                    onAction(TodayAction.START_SETUP)
                }
            }
            TodayDestination.PRIVACY -> {
                PermissionEducationPanel(
                    Res.string.today_privacy_title, Res.string.today_privacy_body,
                )
                Text(stringResource(Res.string.today_processing_unavailable))
                PrivacyIndicator()
                TodayActionButton(Res.string.today_continue, !state.isSaving) {
                    onAction(TodayAction.CONTINUE_PRIVACY)
                }
            }
            TodayDestination.ACCESS -> {
                NotificationAccessEducation(state, onAction)
                TodayActionButton(
                    Res.string.today_continue,
                    !state.isSaving && state.access == NotificationAccessState.GRANTED,
                ) { onAction(TodayAction.CONTINUE_ACCESS) }
                TodayActionButton(Res.string.today_not_now, !state.isSaving, secondary = true) {
                    onAction(TodayAction.SKIP_SETUP)
                }
            }
            TodayDestination.ALERTS -> {
                ProposalAlertsEducation(state, onAction)
                TodayActionButton(
                    if (state.alerts == ProposalAlertPermission.GRANTED) {
                        Res.string.today_finish
                    } else {
                        Res.string.today_not_now
                    },
                    !state.isSaving,
                    secondary = state.alerts != ProposalAlertPermission.GRANTED,
                ) { onAction(TodayAction.FINISH_SETUP) }
            }
            TodayDestination.PERMISSIONS -> {
                TodayHeading(stringResource(Res.string.today_permissions))
                NotificationAccessEducation(state, onAction)
                ProposalAlertsEducation(state, onAction)
                PrivacyIndicator()
                TodayActionButton(Res.string.today_back, secondary = true) {
                    onAction(TodayAction.BACK_TO_TODAY)
                }
            }
            TodayDestination.TODAY -> Unit
        }
        if (state.destination in setOf(
                TodayDestination.PRIVACY, TodayDestination.ACCESS, TodayDestination.ALERTS,
            )
        ) {
            TodayActionButton(Res.string.today_previous, !state.isSaving, secondary = true) {
                onAction(TodayAction.GO_BACK)
            }
        }
    }
}

@Composable
internal fun PermissionEducationPanel(title: StringResource, body: StringResource) {
    Column(verticalArrangement = Arrangement.spacedBy(NotiMateSpacing.space12)) {
        TodayHeading(stringResource(title))
        Text(stringResource(body), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun NotificationAccessEducation(state: TodayUiState, onAction: (TodayAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(NotiMateSpacing.space12)) {
        PermissionEducationPanel(Res.string.today_access_title, Res.string.today_access_body)
        Text(stringResource(when (state.access) {
            NotificationAccessState.CHECKING -> Res.string.today_access_checking
            NotificationAccessState.NOT_GRANTED -> Res.string.today_access_not_granted
            NotificationAccessState.GRANTED -> Res.string.today_access_granted
            NotificationAccessState.REVOKED -> Res.string.today_access_revoked
        }))
        Text(stringResource(Res.string.today_processing_unavailable))
        TodayActionButton(
            Res.string.today_open_settings,
            !state.isSaving,
            secondary = state.access == NotificationAccessState.GRANTED,
        ) {
            onAction(TodayAction.OPEN_ACCESS_SETTINGS)
        }
    }
}

@Composable
internal fun ProposalAlertsEducation(state: TodayUiState, onAction: (TodayAction) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(NotiMateSpacing.space12)) {
        PermissionEducationPanel(Res.string.today_alerts_title, Res.string.today_alerts_body)
        Text(stringResource(alertPermissionLabel(state.alerts)))
        Text(
            stringResource(Res.string.today_alert_delivery_unavailable),
            style = MaterialTheme.typography.bodyMedium,
        )
        TodayActionButton(
            if (state.alerts == ProposalAlertPermission.NOT_REQUESTED) {
                Res.string.today_enable_alerts
            } else {
                Res.string.today_alert_settings
            },
            !state.isSaving,
            secondary = state.alerts == ProposalAlertPermission.GRANTED,
        ) { onAction(TodayAction.ENABLE_ALERTS) }
    }
}

internal fun alertPermissionLabel(permission: ProposalAlertPermission): StringResource =
    when (permission) {
        ProposalAlertPermission.NOT_REQUESTED -> Res.string.today_alerts_not_requested
        ProposalAlertPermission.GRANTED -> Res.string.today_alerts_granted
        ProposalAlertPermission.DENIED -> Res.string.today_alerts_denied
        ProposalAlertPermission.APP_DISABLED -> Res.string.today_alerts_app_disabled
        ProposalAlertPermission.CHANNEL_DISABLED -> Res.string.today_alerts_channel_disabled
    }
