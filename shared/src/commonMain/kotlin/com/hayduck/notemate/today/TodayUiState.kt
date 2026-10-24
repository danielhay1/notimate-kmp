package com.hayduck.notemate.today

import com.hayduck.notemate.domain.activity.ActivityEvent
import com.hayduck.notemate.domain.proposal.CalendarDate
import com.hayduck.notemate.domain.proposal.CalendarTime
import com.hayduck.notemate.domain.proposal.ProposalState

enum class ProposalAlertPermission {
    NOT_REQUESTED, GRANTED, DENIED, APP_DISABLED, CHANNEL_DISABLED,
}

/** Permission facts only; access and outgoing alerts are independent capabilities. */
data class PermissionSnapshot(
    val hasNotificationAccess: Boolean,
    val alerts: ProposalAlertPermission,
)

enum class NotificationAccessState { CHECKING, NOT_GRANTED, GRANTED, REVOKED }
enum class TodayDestination { WELCOME, PRIVACY, ACCESS, ALERTS, TODAY, PERMISSIONS }
enum class OnboardingLoadState { LOADING, READY, FAILED }
enum class TodayMessage {
    SAVE_FAILED, ACTION_FAILED, SETTINGS_UNAVAILABLE, PERMISSION_CHECK_FAILED,
}
enum class TodayProcessingState {
    CHECKING, CONFIGURATION_UNAVAILABLE, PERMISSION_REQUIRED, PAUSED, PROCESSOR_UNAVAILABLE,
}

sealed interface TodayConfigurationUiState {
    data object Loading : TodayConfigurationUiState
    data object Failed : TodayConfigurationUiState
    data class Ready(
        val profileName: String,
        val isPaused: Boolean,
        val monitoredSourceCount: Int,
        val enabledAutomationCount: Int,
    ) : TodayConfigurationUiState
}

/** Structured local fields only; no source payload or model response belongs in presentation. */
class TodayProposalUiState(
    val id: String,
    val title: String?,
    val date: CalendarDate?,
    val time: CalendarTime?,
    val location: String?,
    val sourceName: String,
    val profileName: String,
    val automationName: String,
    val state: ProposalState,
) {
    override fun toString(): String = "TodayProposalUiState(state=$state, content=[REDACTED])"
}

data class RecentOutcomeUiState(
    val id: String,
    val event: ActivityEvent,
    val occurredAtEpochMilliseconds: Long,
)

sealed interface TodayResultsUiState {
    data object Loading : TodayResultsUiState
    data object Failed : TodayResultsUiState
    data class Ready(
        val proposals: List<TodayProposalUiState>,
        val recentOutcomes: List<RecentOutcomeUiState>,
    ) : TodayResultsUiState
}

/** Independent repository sections preserve usable results when configuration or access fails. */
data class TodayUiState(
    val onboarding: OnboardingLoadState = OnboardingLoadState.LOADING,
    val destination: TodayDestination = TodayDestination.WELCOME,
    val access: NotificationAccessState = NotificationAccessState.CHECKING,
    val alerts: ProposalAlertPermission = ProposalAlertPermission.NOT_REQUESTED,
    val configuration: TodayConfigurationUiState = TodayConfigurationUiState.Loading,
    val results: TodayResultsUiState = TodayResultsUiState.Loading,
    val isSaving: Boolean = false,
    val message: TodayMessage? = null,
) {
    val processing: TodayProcessingState get() = when {
        configuration == TodayConfigurationUiState.Failed ->
            TodayProcessingState.CONFIGURATION_UNAVAILABLE
        configuration == TodayConfigurationUiState.Loading ||
            access == NotificationAccessState.CHECKING -> TodayProcessingState.CHECKING
        access != NotificationAccessState.GRANTED -> TodayProcessingState.PERMISSION_REQUIRED
        (configuration as TodayConfigurationUiState.Ready).isPaused -> TodayProcessingState.PAUSED
        else -> TodayProcessingState.PROCESSOR_UNAVAILABLE
    }
}

enum class TodayAction {
    START_SETUP, CONTINUE_PRIVACY, CONTINUE_ACCESS, OPEN_ACCESS_SETTINGS,
    ENABLE_ALERTS, SKIP_SETUP, FINISH_SETUP, OPEN_PERMISSIONS, BACK_TO_TODAY,
    GO_BACK, PAUSE, RESUME, RETRY_ONBOARDING, RETRY_CONFIGURATION, RETRY_RESULTS, DISMISS_MESSAGE,
}

/** Executed once by the platform owner, never by a shared rendering branch. */
enum class TodayEffect { OPEN_ACCESS_SETTINGS, REQUEST_ALERT_PERMISSION, OPEN_ALERT_SETTINGS }
