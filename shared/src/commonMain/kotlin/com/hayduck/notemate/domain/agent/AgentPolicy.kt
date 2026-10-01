package com.hayduck.notemate.domain.agent

import com.hayduck.notemate.domain.automation.Automation
import com.hayduck.notemate.domain.automation.AutomationAction
import com.hayduck.notemate.domain.automation.AutomationProfiles
import com.hayduck.notemate.domain.notification.NotificationClassification
import com.hayduck.notemate.domain.notification.NotificationSource

/** Advisory inputs only; [AgentPolicy] never executes actions or retains notification content. */
data class AgentPolicyInput(
    val classification: NotificationClassification,
    val confidence: Double,
    val isSensitive: Boolean = false,
    val isAmbiguous: Boolean = false,
    val isSchemaValid: Boolean,
    val requiresLocalInference: Boolean = false,
    val isLocalInferenceAvailable: Boolean = true,
    val isDeviceConstrained: Boolean = false,
) {
    init {
        require(confidence.isFinite() && confidence in 0.0..1.0) {
            "Confidence must be finite and between zero and one."
        }
    }
}

/** Source-only admission must run before the Android boundary reads content. */
fun canObserveNotification(
    source: NotificationSource,
    monitoredApplicationIds: Set<String>,
    isProcessingPaused: Boolean,
): Boolean = !isProcessingPaused && source.applicationId in monitoredApplicationIds

/** Conservative routing with a caller-owned confidence threshold; never authorizes a write. */
class AgentPolicy(private val minimumConfidence: Double) {
    init {
        require(minimumConfidence.isFinite() && minimumConfidence in 0.0..1.0) {
            "Minimum confidence must be finite and between zero and one."
        }
    }

    /** Routes only an Automation owned by the selected Profile. */
    fun evaluateSelected(
        source: NotificationSource,
        monitoredApplicationIds: Set<String>,
        isProcessingPaused: Boolean,
        profiles: AutomationProfiles,
        automationId: String,
        input: AgentPolicyInput,
    ): AgentOutcome {
        val automation = profiles.selectedProfile.automations.find { it.id == automationId }
            ?: return AgentOutcome.IGNORE
        return evaluate(source, monitoredApplicationIds, isProcessingPaused, automation, input)
    }

    /** Rechecks source admission even when the boundary has already applied it. */
    internal fun evaluate(
        source: NotificationSource,
        monitoredApplicationIds: Set<String>,
        isProcessingPaused: Boolean,
        automation: Automation,
        input: AgentPolicyInput,
    ): AgentOutcome {
        if (!canObserveNotification(source, monitoredApplicationIds, isProcessingPaused) ||
            !automation.isEnabled || !automation.sourceSelector.matches(source)
        ) return AgentOutcome.IGNORE

        if (!automation.matchesClassification(input.classification)) return AgentOutcome.IGNORE

        if (input.isSensitive || input.isAmbiguous || !input.isSchemaValid ||
            input.confidence < minimumConfidence
        ) return AgentOutcome.REVIEW

        if (input.requiresLocalInference) {
            if (!input.isLocalInferenceAvailable) return AgentOutcome.REVIEW
            if (input.isDeviceConstrained) return AgentOutcome.DEFER
        }

        return when (input.classification) {
            NotificationClassification.UNKNOWN,
            NotificationClassification.REVIEW_NEEDED -> AgentOutcome.REVIEW
            NotificationClassification.IGNORED -> AgentOutcome.IGNORE
            NotificationClassification.POSSIBLE_CALENDAR_EVENT -> when (automation.action) {
                AutomationAction.PROPOSE_CALENDAR_EVENT -> AgentOutcome.DRAFT
                AutomationAction.IGNORE -> AgentOutcome.IGNORE
                AutomationAction.GROUP_FOR_REVIEW -> AgentOutcome.REVIEW
            }
        }
    }
}
