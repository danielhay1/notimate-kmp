package com.hayduck.notemate.designsystem

import androidx.compose.runtime.Immutable

/** Display-only status. Explanations name the affected capability and safe available recovery. */
@Immutable
sealed interface ProcessingStatusUiState {
    data class Active(val profileName: String) : ProcessingStatusUiState
    data object Paused : ProcessingStatusUiState
    data object PermissionRequired : ProcessingStatusUiState
    data class Degraded(val explanation: String) : ProcessingStatusUiState
    data class Unavailable(
        val explanation: String,
        val recovery: ProcessingRecovery,
    ) : ProcessingStatusUiState
}

enum class ProcessingRecovery { TRY_AGAIN, REVIEW_LATER }

enum class ProcessingStatusAction {
    RESUME, OPEN_NOTIFICATION_ACCESS_SETTINGS, USE_BASIC_PROCESSING, TRY_AGAIN, REVIEW_LATER,
}
