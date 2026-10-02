package com.hayduck.notemate.domain.extraction

import com.hayduck.notemate.domain.notification.NotificationClassification

/**
 * Classification is independent of the suggested action. Model confidence is an uncalibrated score;
 * it can lower, but never raise, the caller's confidence assessment. No source text is retained.
 */
class NotificationAnalysis(
    val classification: NotificationClassification,
    val suggestedAction: SuggestedNotificationAction?,
    val confidence: Double,
    val isSensitive: Boolean,
) {
    init {
        require(confidence.isFinite() && confidence in 0.0..1.0) { "Invalid model confidence." }
    }

    override fun toString(): String = "NotificationAnalysis([REDACTED])"
}

internal fun NotificationAnalysis.isSchemaValid(): Boolean = when (val action = suggestedAction) {
    is SuggestedNotificationAction.CalendarEvent ->
        classification == NotificationClassification.POSSIBLE_CALENDAR_EVENT &&
            action.calendar.isSchemaValid()
    null -> classification != NotificationClassification.POSSIBLE_CALENDAR_EVENT
}
