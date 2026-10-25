package com.hayduck.notemate.domain.extraction

import com.hayduck.notemate.domain.activity.ActivityEvent
import com.hayduck.notemate.domain.activity.ActivityRecord
import com.hayduck.notemate.domain.agent.AgentOutcome
import com.hayduck.notemate.domain.agent.AgentPolicy
import com.hayduck.notemate.domain.agent.AgentPolicyInput
import com.hayduck.notemate.domain.automation.AutomationAction
import com.hayduck.notemate.domain.automation.ConfirmationPolicy
import com.hayduck.notemate.domain.configuration.LocalConfiguration
import com.hayduck.notemate.domain.notification.NotificationClassification
import com.hayduck.notemate.domain.notification.ObservedNotification
import com.hayduck.notemate.domain.proposal.CalendarProposal
import com.hayduck.notemate.domain.proposal.ProposalOrigin
import com.hayduck.notemate.domain.proposal.validationIssues
import kotlin.coroutines.cancellation.CancellationException

/** Caller-owned confidence assessment and sensitivity flag; this slice supplies no calibration. */
data class ExtractionAssessment(val confidence: Double, val isSensitive: Boolean) {
    init {
        require(confidence.isFinite() && confidence in 0.0..1.0) { "Invalid confidence." }
    }
}

/** Explicit local identifiers, source label, clock, and expiry policy supplied by the caller. */
class ExtractionProposalRequest(
    val proposalId: String,
    val automationId: String,
    val applicationName: String,
    val createdAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long?,
) {
    init {
        require(listOf(proposalId, automationId, applicationName).all { it.isNotBlank() }) {
            "Proposal metadata must not be blank."
        }
        require(expiresAtEpochMilliseconds == null ||
            expiresAtEpochMilliseconds > createdAtEpochMilliseconds) { "Invalid expiry." }
    }

    override fun toString(): String = "ExtractionProposalRequest([REDACTED])"
}

/** Validated transient OTP ready for immediate clipboard copy; callers must never persist [code]. */
class OtpClipboardCopy(
    val code: String,
    val origin: ProposalOrigin,
    val createdAtEpochMilliseconds: Long,
) {
    init {
        require(OtpExtraction(code).isSchemaValid()) { "Invalid OTP code." }
    }

    /** Produces content-free audit metadata for a completed clipboard copy; [code] is excluded. */
    fun activityRecord(activityId: String): ActivityRecord = ActivityRecord(
        id = activityId,
        event = ActivityEvent.OTP_COPIED_TO_CLIPBOARD,
        occurredAtEpochMilliseconds = createdAtEpochMilliseconds,
        proposalId = null,
        profileId = origin.profileId,
        automationId = origin.automationId,
        applicationId = origin.applicationId,
    )

    override fun equals(other: Any?): Boolean =
        other is OtpClipboardCopy &&
            code == other.code &&
            origin == other.origin &&
            createdAtEpochMilliseconds == other.createdAtEpochMilliseconds

    override fun hashCode(): Int {
        var result = code.hashCode()
        result = 31 * result + origin.hashCode()
        result = 31 * result + createdAtEpochMilliseconds.hashCode()
        return result
    }

    override fun toString(): String = "OtpClipboardCopy([REDACTED])"
}

sealed interface NotificationExtractionOutcome {
    data class Proposal(val proposal: CalendarProposal) : NotificationExtractionOutcome
    data class OtpClipboard(val copy: OtpClipboardCopy) : NotificationExtractionOutcome {
        override fun toString(): String = "OtpClipboard([REDACTED])"
    }
    data object Ignored : NotificationExtractionOutcome
    data class ReviewNeeded(val reason: ExtractionReviewReason) : NotificationExtractionOutcome
    /** No input is retained or retry scheduled; callers need approved structured work. */
    data class Deferred(val reason: ExtractionDeferralReason) : NotificationExtractionOutcome
    data object Failed : NotificationExtractionOutcome
}

enum class ExtractionDeferralReason { DEVICE_CONSTRAINED, LOCAL_AI_DEFERRED }

/**
 * Resolves one selected Automation using an atomic configuration snapshot, without persistence.
 * Adapters honor [NotificationExtractor]'s transient-input contract and own their threads.
 * Callers own durable results before enabling production capture. New action types extend the typed
 * suggestion, decoder, and proposal mapping; they cannot bypass [AgentPolicy].
 */
class NotificationExtractionPipeline(
    private val extractor: NotificationExtractor,
    private val fallback: NotificationExtractor,
    private val policy: AgentPolicy,
) {
    init {
        require(!fallback.requiresHeavyWork) { "Fallback must not require heavy work." }
    }

    suspend fun process(
        notification: ObservedNotification,
        configuration: LocalConfiguration?,
        request: ExtractionProposalRequest,
        assessment: ExtractionAssessment,
        device: ExtractionDeviceConditions,
        localAiState: LocalAiState,
        context: NotificationInterpretationContext? = null,
    ): NotificationExtractionOutcome {
        if (configuration == null) return review(ExtractionReviewReason.CONFIGURATION_UNAVAILABLE)
        if (!configuration.asCaptureSettings().admits(notification.source)) {
            return NotificationExtractionOutcome.Ignored
        }
        val profile = configuration.profiles.selectedProfile
        val automation = profile.automations.find { it.id == request.automationId }
            ?: return NotificationExtractionOutcome.Ignored
        if (!automation.isEnabled || !automation.sourceSelector.matches(notification.source)) {
            return NotificationExtractionOutcome.Ignored
        }
        if (!notification.isWithinExtractionLimits()) {
            return review(ExtractionReviewReason.INPUT_LIMIT)
        }
        val requiresLocalModelOnly = automation.action == AutomationAction.COPY_OTP_TO_CLIPBOARD
        if (requiresLocalModelOnly && !extractor.requiresHeavyWork) {
            return review(ExtractionReviewReason.LOCAL_AI_UNAVAILABLE)
        }
        val blocked = heavyWorkFallback(device, localAiState)
        if (requiresLocalModelOnly && blocked != null) return blocked
        val result = if (blocked == null) runExtractor(extractor, notification, context)
            else runExtractor(fallback, notification, context)
        val canFallback = extractor.requiresHeavyWork && blocked == null && !requiresLocalModelOnly
        val recovered = if (canFallback && (result == ExtractionResult.Failed ||
            result is ExtractionResult.Unavailable)
        ) runExtractor(fallback, notification, context) else result
        val analysis = (recovered as? ExtractionResult.Analysis)?.analysis
        val calendar = when (recovered) {
            is ExtractionResult.Calendar -> recovered.extraction
            is ExtractionResult.Analysis -> when (val action = recovered.analysis.suggestedAction) {
                is SuggestedNotificationAction.CalendarEvent -> action.calendar
                else -> null
            }
            else -> null
        }
        val otp = when (recovered) {
            is ExtractionResult.Analysis -> when (val action = recovered.analysis.suggestedAction) {
                is SuggestedNotificationAction.OtpCode -> action.otp
                else -> null
            }
            else -> null
        }
        val schemaValid = (analysis?.isSchemaValid() ?: true) &&
            (calendar?.isSchemaValid() ?: true) &&
            (otp?.isSchemaValid() ?: true)
        val confidence = minOf(
            assessment.confidence, analysis?.confidence ?: assessment.confidence,
        )
        val isSensitive = assessment.isSensitive || analysis?.isSensitive == true
        val classification = when (recovered) {
            is ExtractionResult.Analysis -> recovered.analysis.classification
            is ExtractionResult.Calendar -> NotificationClassification.POSSIBLE_CALENDAR_EVENT
            ExtractionResult.Ignored -> NotificationClassification.IGNORED
            ExtractionResult.Unknown -> NotificationClassification.UNKNOWN
            else -> NotificationClassification.REVIEW_NEEDED
        }
        val decision = policy.evaluateSelected(
            notification.source, configuration.monitoredApplicationIds, configuration.isPaused,
            configuration.profiles, automation.id,
            AgentPolicyInput(
                classification = classification,
                confidence = confidence,
                isSensitive = isSensitive,
                isAmbiguous = calendar?.let {
                    it.fields.validationIssues(it.ambiguousFields).isNotEmpty()
                } ?: false,
                isSchemaValid = schemaValid,
            ),
        )
        if (decision == AgentOutcome.IGNORE) return NotificationExtractionOutcome.Ignored
        if (isSensitive) return review(ExtractionReviewReason.POLICY_REVIEW)
        if (!schemaValid) return review(ExtractionReviewReason.INVALID_OUTPUT)
        val origin = ProposalOrigin(
            profile.id, profile.name, automation.id, automation.name,
            notification.source.applicationId, request.applicationName,
        )
        if (otp != null) {
            if (decision != AgentOutcome.AUTOMATIC_ACTION) {
                return review(ExtractionReviewReason.POLICY_REVIEW)
            }
            return NotificationExtractionOutcome.OtpClipboard(
                OtpClipboardCopy(otp.code, origin, request.createdAtEpochMilliseconds),
            )
        }
        if (calendar != null) {
            if (automation.action != AutomationAction.PROPOSE_CALENDAR_EVENT ||
                automation.confirmationPolicy != ConfirmationPolicy.ALWAYS_REVIEW ||
                (decision == AgentOutcome.REVIEW &&
                    calendar.fields.validationIssues(calendar.ambiguousFields).isEmpty())
            ) return review(ExtractionReviewReason.POLICY_REVIEW)
            return NotificationExtractionOutcome.Proposal(CalendarProposal.draft(
                request.proposalId, origin, calendar.fields, request.createdAtEpochMilliseconds,
                request.expiresAtEpochMilliseconds, calendar.ambiguousFields,
            ).validate(request.createdAtEpochMilliseconds))
        }
        return when (recovered) {
            is ExtractionResult.Analysis -> review(ExtractionReviewReason.POLICY_REVIEW)
            is ExtractionResult.Calendar -> review(ExtractionReviewReason.INVALID_OUTPUT)
            ExtractionResult.Ignored -> review(ExtractionReviewReason.POLICY_REVIEW)
            ExtractionResult.Unknown -> blocked ?: when (result) {
                is ExtractionResult.Unavailable -> review(result.reason)
                ExtractionResult.Failed -> NotificationExtractionOutcome.Failed
                else -> review(ExtractionReviewReason.UNSUPPORTED_FORMAT)
            }
            is ExtractionResult.ReviewNeeded -> review(recovered.reason)
            is ExtractionResult.Unavailable -> review(recovered.reason)
            ExtractionResult.Failed -> NotificationExtractionOutcome.Failed
        }
    }

    private fun heavyWorkFallback(
        device: ExtractionDeviceConditions,
        state: LocalAiState,
    ): NotificationExtractionOutcome? {
        if (!extractor.requiresHeavyWork) return null
        return when (state) {
            LocalAiState.READY -> if (device.allowsHeavyWork) null else
                NotificationExtractionOutcome.Deferred(ExtractionDeferralReason.DEVICE_CONSTRAINED)
            LocalAiState.DEFERRED ->
                NotificationExtractionOutcome.Deferred(ExtractionDeferralReason.LOCAL_AI_DEFERRED)
            LocalAiState.FAILED -> NotificationExtractionOutcome.Failed
            else -> review(ExtractionReviewReason.LOCAL_AI_UNAVAILABLE)
        }
    }

    private suspend fun runExtractor(
        adapter: NotificationExtractor,
        notification: ObservedNotification,
        context: NotificationInterpretationContext?,
    ): ExtractionResult = try {
        adapter.extract(notification, context)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        ExtractionResult.Failed
    }

    private fun review(reason: ExtractionReviewReason): NotificationExtractionOutcome =
        NotificationExtractionOutcome.ReviewNeeded(reason)
}
