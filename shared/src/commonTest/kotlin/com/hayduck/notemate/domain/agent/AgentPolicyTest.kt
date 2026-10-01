package com.hayduck.notemate.domain.agent

import com.hayduck.notemate.domain.automation.Automation
import com.hayduck.notemate.domain.automation.AutomationAction
import com.hayduck.notemate.domain.automation.AutomationProfile
import com.hayduck.notemate.domain.automation.AutomationProfiles
import com.hayduck.notemate.domain.automation.NotificationSourceSelector
import com.hayduck.notemate.domain.notification.NotificationClassification
import com.hayduck.notemate.domain.notification.NotificationSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class AgentPolicyTest {
    private val source = NotificationSource("com.example.synthetic")
    private val automation = Automation(
        id = "calendar",
        name = "Calendar",
        isEnabled = true,
        sourceSelector = NotificationSourceSelector.AnyMonitoredApplication,
        action = AutomationAction.PROPOSE_CALENDAR_EVENT,
    )
    private val policy = AgentPolicy(minimumConfidence = 0.8)
    private val safe = AgentPolicyInput(
        classification = NotificationClassification.POSSIBLE_CALENDAR_EVENT,
        confidence = 0.9,
        isSchemaValid = true,
    )

    @Test
    fun safeEventCreatesDraftNeverExternalWrite() {
        assertEquals(AgentOutcome.DRAFT, evaluate(safe))
    }

    @Test
    fun uncertainSensitiveAndInvalidResultsRequireReview() {
        listOf(
            safe.copy(confidence = 0.7),
            safe.copy(isSensitive = true),
            safe.copy(isAmbiguous = true),
            safe.copy(isSchemaValid = false),
            safe.copy(classification = NotificationClassification.UNKNOWN),
            safe.copy(classification = NotificationClassification.REVIEW_NEEDED),
        ).forEach { assertEquals(AgentOutcome.REVIEW, evaluate(it)) }
    }

    @Test
    fun inferenceUnavailableReviewsAndConstrainedDeviceDefers() {
        assertEquals(AgentOutcome.REVIEW, evaluate(safe.copy(
            requiresLocalInference = true, isLocalInferenceAvailable = false,
        )))
        assertEquals(AgentOutcome.DEFER, evaluate(safe.copy(
            requiresLocalInference = true, isDeviceConstrained = true,
        )))
        assertEquals(AgentOutcome.DRAFT, evaluate(safe.copy(isDeviceConstrained = true)))
    }

    @Test
    fun admissionRejectsPausedAndUnmonitoredSourcesWithoutContent() {
        assertFalse(canObserveNotification(source, emptySet(), false))
        assertFalse(canObserveNotification(source, setOf(source.applicationId), true))
        assertEquals(AgentOutcome.IGNORE, evaluate(safe, paused = true))
        assertEquals(AgentOutcome.IGNORE, policy.evaluate(
            source, emptySet(), false, automation, safe,
        ))
    }

    @Test
    fun inactiveAutomationCannotBeRouted() {
        val profiles = AutomationProfiles(listOf(
            AutomationProfile("personal", "Personal", automations = listOf(automation)),
        ), "personal")
        assertEquals(AgentOutcome.IGNORE, policy.evaluateSelected(
            source, setOf(source.applicationId), false, profiles, "inactive", safe,
        ))
    }

    @Test
    fun confidenceRejectsInvalidAndNonFiniteNumbers() {
        listOf(-0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> { safe.copy(confidence = invalid) }
            assertFailsWith<IllegalArgumentException> { AgentPolicy(invalid) }
        }
    }

    @Test
    fun conditionsAcceptAnyAllowedClassificationButExclusionsWin() {
        val configured = Automation(
            "condition", "Condition", true,
            NotificationSourceSelector.AnyMonitoredApplication,
            AutomationAction.PROPOSE_CALENDAR_EVENT,
            conditions = setOf(
                NotificationClassification.POSSIBLE_CALENDAR_EVENT,
                NotificationClassification.UNKNOWN,
            ),
            exclusions = setOf(NotificationClassification.UNKNOWN),
        )
        assertEquals(true, configured.matchesClassification(
            NotificationClassification.POSSIBLE_CALENDAR_EVENT,
        ))
        assertEquals(false, configured.matchesClassification(NotificationClassification.UNKNOWN))
        assertEquals(false, configured.matchesClassification(NotificationClassification.IGNORED))
        assertEquals(AgentOutcome.IGNORE, policy.evaluate(
            source, setOf(source.applicationId), false, configured,
            safe.copy(classification = NotificationClassification.UNKNOWN, isSensitive = true),
        ))
    }

    private fun evaluate(input: AgentPolicyInput, paused: Boolean = false): AgentOutcome =
        policy.evaluate(source, setOf(source.applicationId), paused, automation, input)
}
