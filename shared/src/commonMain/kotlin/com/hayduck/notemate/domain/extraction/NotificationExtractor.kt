package com.hayduck.notemate.domain.extraction

import com.hayduck.notemate.domain.notification.ObservedNotification
import com.hayduck.notemate.domain.proposal.CalendarField
import com.hayduck.notemate.domain.proposal.CalendarFields

/** Local extraction; raw input must not escape, be logged, or cause side effects. */
interface NotificationExtractor {
    val requiresHeavyWork: Boolean
    suspend fun extract(
        notification: ObservedNotification,
        context: NotificationInterpretationContext? = null,
    ): ExtractionResult
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

/** Advisory one-time verification code; must pass schema and policy validation before copy. */
class OtpExtraction(val code: String) {
    override fun equals(other: Any?): Boolean = other is OtpExtraction && code == other.code
    override fun hashCode(): Int = code.hashCode()
    override fun toString(): String = "OtpExtraction([REDACTED])"
}

sealed interface ExtractionResult {
    data class Analysis(val analysis: NotificationAnalysis) : ExtractionResult
    data class Calendar(val extraction: CalendarExtraction) : ExtractionResult
    data object Ignored : ExtractionResult
    data object Unknown : ExtractionResult
    data class ReviewNeeded(val reason: ExtractionReviewReason) : ExtractionResult
    /** Runtime cannot interpret this input; deterministic fallback is permitted. */
    data class Unavailable(val reason: ExtractionReviewReason) : ExtractionResult
    data object Failed : ExtractionResult
}

enum class ExtractionReviewReason {
    INPUT_LIMIT,
    INVALID_OUTPUT,
    UNSUPPORTED_FORMAT,
    POLICY_REVIEW,
    CONFIGURATION_UNAVAILABLE,
    LOCAL_AI_UNAVAILABLE,
    CONTEXT_UNAVAILABLE,
    INFERENCE_BUSY,
    INFERENCE_TIMED_OUT,
    UNSUPPORTED_ACTION,
}

internal const val MAXIMUM_NOTIFICATION_TITLE_LENGTH = 256
internal const val MAXIMUM_NOTIFICATION_BODY_LENGTH = 4096
internal const val MAXIMUM_EXTRACTED_TEXT_LENGTH = 256
internal const val MINIMUM_OTP_CODE_LENGTH = 4
internal const val MAXIMUM_OTP_CODE_LENGTH = 8
internal const val MAXIMUM_MODEL_OUTPUT_LENGTH = 8192

internal fun ObservedNotification.isWithinExtractionLimits(): Boolean =
    (content.title?.length ?: 0) <= MAXIMUM_NOTIFICATION_TITLE_LENGTH &&
        (content.body?.length ?: 0) <= MAXIMUM_NOTIFICATION_BODY_LENGTH

internal fun CalendarExtraction.isSchemaValid(): Boolean =
    listOf(fields.title, fields.location).all { text ->
        text == null || (text.isNotBlank() && text.length <= MAXIMUM_EXTRACTED_TEXT_LENGTH &&
            text.none { it.isISOControl() })
    }

internal fun OtpExtraction.isSchemaValid(): Boolean =
    code.length in MINIMUM_OTP_CODE_LENGTH..MAXIMUM_OTP_CODE_LENGTH &&
        code.all { it in '0'..'9' || it in 'A'..'Z' || it in 'a'..'z' } &&
        code.any { it in '0'..'9' }
