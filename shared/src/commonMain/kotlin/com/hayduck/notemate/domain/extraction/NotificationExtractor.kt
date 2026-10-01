package com.hayduck.notemate.domain.extraction

import com.hayduck.notemate.domain.notification.ObservedNotification
import com.hayduck.notemate.domain.proposal.CalendarField
import com.hayduck.notemate.domain.proposal.CalendarFields

/** Local extraction; raw input must not escape, be logged, or cause side effects. */
interface NotificationExtractor {
    val requiresHeavyWork: Boolean
    fun extract(notification: ObservedNotification): ExtractionResult
}

/** Advisory fields; adapter output must pass validation before proposal creation. */
class CalendarExtraction(
    val fields: CalendarFields,
    ambiguousFields: Set<CalendarField>,
) {
    private val ambiguity = ambiguousFields.toSet()
    val ambiguousFields: Set<CalendarField> get() = ambiguity.toSet()

    override fun toString(): String = "CalendarExtraction([REDACTED])"
}

sealed interface ExtractionResult {
    data class Calendar(val extraction: CalendarExtraction) : ExtractionResult
    data object Ignored : ExtractionResult
    data object Unknown : ExtractionResult
    data class ReviewNeeded(val reason: ExtractionReviewReason) : ExtractionResult
    data object Failed : ExtractionResult
}

enum class ExtractionReviewReason {
    INPUT_LIMIT,
    INVALID_OUTPUT,
    UNSUPPORTED_FORMAT,
    POLICY_REVIEW,
    CONFIGURATION_UNAVAILABLE,
    LOCAL_AI_UNAVAILABLE,
}

internal const val MAXIMUM_NOTIFICATION_TITLE_LENGTH = 256
internal const val MAXIMUM_NOTIFICATION_BODY_LENGTH = 4096
internal const val MAXIMUM_EXTRACTED_TEXT_LENGTH = 256
internal const val MAXIMUM_MODEL_OUTPUT_LENGTH = 8192

internal fun ObservedNotification.isWithinExtractionLimits(): Boolean =
    (content.title?.length ?: 0) <= MAXIMUM_NOTIFICATION_TITLE_LENGTH &&
        (content.body?.length ?: 0) <= MAXIMUM_NOTIFICATION_BODY_LENGTH

internal fun CalendarExtraction.isSchemaValid(): Boolean =
    listOf(fields.title, fields.location).all { text ->
        text == null || (text.isNotBlank() && text.length <= MAXIMUM_EXTRACTED_TEXT_LENGTH &&
            text.none { it.isISOControl() })
    }
