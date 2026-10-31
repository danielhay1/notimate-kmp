package com.hayduck.notemate.designsystem

import androidx.compose.runtime.Immutable

/**
 * Local structured summary, never raw notification text. Callers supply localized display text,
 * including date/time formatting and any uncertainty or safe recovery explanation.
 * Expired summaries expose no lifecycle actions; the caller must also enforce domain guards.
 */
@Immutable
class ProposalCardUiState(
    val title: String,
    val summary: String,
    val sourceName: String,
    val profileName: String,
    val automationName: String,
    val status: ProposalCardStatus,
    val explanation: String,
) {
    override fun toString(): String = "ProposalCardUiState(status=$status, content=[REDACTED])"
}

enum class ProposalCardStatus { DRAFT, NEEDS_REVIEW, READY, DEFERRED, FAILED, EXPIRED }

/** Requests a caller-owned action; rendering a card never changes a Proposal or opens Calendar. */
enum class ProposalCardAction { REVIEW, DISMISS, RETRY }
