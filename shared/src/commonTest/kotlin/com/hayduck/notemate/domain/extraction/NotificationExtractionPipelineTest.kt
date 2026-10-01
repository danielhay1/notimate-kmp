package com.hayduck.notemate.domain.extraction

import com.hayduck.notemate.domain.agent.AgentPolicy
import com.hayduck.notemate.domain.automation.Automation
import com.hayduck.notemate.domain.automation.AutomationAction
import com.hayduck.notemate.domain.automation.AutomationProfile
import com.hayduck.notemate.domain.automation.AutomationProfiles
import com.hayduck.notemate.domain.automation.NotificationSourceSelector
import com.hayduck.notemate.domain.configuration.LocalConfiguration
import com.hayduck.notemate.domain.notification.CaptureResult
import com.hayduck.notemate.domain.notification.NotificationCaptureCoordinator
import com.hayduck.notemate.domain.notification.NotificationCaptureSettings
import com.hayduck.notemate.domain.notification.NotificationCaptureSink
import com.hayduck.notemate.domain.notification.NotificationClassification
import com.hayduck.notemate.domain.notification.NotificationContent
import com.hayduck.notemate.domain.notification.NotificationSource
import com.hayduck.notemate.domain.notification.ObservedNotification
import com.hayduck.notemate.domain.proposal.CalendarFields
import com.hayduck.notemate.domain.proposal.ProposalIssue
import com.hayduck.notemate.domain.proposal.ProposalState
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class NotificationExtractionPipelineTest {
    private val rules = RuleBasedCalendarExtractor()
    private val policy = AgentPolicy(0.8)
    private val assessment = ExtractionAssessment(0.9, isSensitive = false)
    private val source = NotificationSource("synthetic.app")
    private val notification = ObservedNotification(source, 100,
        NotificationContent("Event: Synthetic demo", "Date: 2028-02-29; Time: 14:30"))
    private val request =
        ExtractionProposalRequest("proposal", "calendar", "Synthetic app", 101, null)
    private val available = ExtractionDeviceConditions(DeviceResourceState.AVAILABLE,
        DeviceResourceState.AVAILABLE, DeviceResourceState.AVAILABLE)

    @Test
    fun validatedExplicitFieldsProduceReadyLocalProposalWithOriginSnapshot() {
        val result = assertIs<NotificationExtractionOutcome.Proposal>(process(rules))
        assertEquals(ProposalState.READY, result.proposal.state)
        assertEquals("personal", result.proposal.origin.profileId)
        assertEquals("Calendar", result.proposal.origin.automationName)
        assertEquals("Synthetic app", result.proposal.origin.applicationName)
        assertEquals(null, result.proposal.expiresAtEpochMilliseconds)
        val renamed = configuration().saveProfile(AutomationProfile("personal", "Renamed",
            automations = configuration().profiles.selectedProfile.automations))
        assertEquals("Renamed", renamed.profiles.selectedProfile.name)
        assertEquals("Personal", result.proposal.origin.profileName)
    }

    @Test
    fun missingAndInvalidFieldsProduceNeedsReviewProposal() {
        val result = assertIs<NotificationExtractionOutcome.Proposal>(process(rules,
            input = notification.copy(content = NotificationContent("Meeting: Synthetic demo",
                "Date: tomorrow; Time: 14:30"))))
        assertEquals(ProposalState.NEEDS_REVIEW, result.proposal.state)
        assertEquals(setOf(ProposalIssue.MISSING_DATE, ProposalIssue.AMBIGUOUS_FIELDS),
            result.proposal.issues)
    }

    @Test
    fun lowConfidenceSensitivityAndInvalidSchemaCannotProduceReadyProposal() {
        assertEquals(review(ExtractionReviewReason.POLICY_REVIEW), process(rules,
            confidence = assessment.copy(confidence = 0.7)))
        assertEquals(review(ExtractionReviewReason.POLICY_REVIEW), process(rules,
            confidence = assessment.copy(isSensitive = true)))
        val invalid = adapter(false) {
            ExtractionResult.Calendar(CalendarExtraction(
                CalendarFields("a".repeat(257), null, null), emptySet(),
            ))
        }
        assertEquals(review(ExtractionReviewReason.INVALID_OUTPUT), process(invalid))
    }

    @Test
    fun unavailableConfigurationAndDeniedSourcesNeverInvokeExtraction() {
        val forbidden = adapter(false) { error("Must not extract") }
        assertEquals(review(ExtractionReviewReason.CONFIGURATION_UNAVAILABLE),
            process(forbidden, config = null))
        listOf(configuration().withPaused(true),
            configuration().withMonitoredApplications(emptySet()),
            configuration(enabled = false), configuration().selectProfile("other"),
        ).forEach {
            assertEquals(NotificationExtractionOutcome.Ignored, process(forbidden, config = it))
        }
        assertEquals(NotificationExtractionOutcome.Ignored, process(forbidden,
            proposalRequest = ExtractionProposalRequest("p", "other-calendar",
                "Synthetic", 101, null)))
    }

    @Test
    fun selectedAutomationConditionsAndActionsStillControlRouting() {
        assertEquals(NotificationExtractionOutcome.Ignored, process(rules,
            config = configuration(action = AutomationAction.IGNORE)))
        assertEquals(NotificationExtractionOutcome.Ignored, process(rules,
            config = configuration(exclusions =
                setOf(NotificationClassification.POSSIBLE_CALENDAR_EVENT))))
        assertEquals(review(ExtractionReviewReason.POLICY_REVIEW), process(rules,
            config = configuration(action = AutomationAction.GROUP_FOR_REVIEW)))
    }

    @Test
    fun basicRulesRemainAvailableInEveryLocalAiStateAndOnConstrainedDevices() {
        LocalAiState.entries.forEach { state ->
            assertIs<NotificationExtractionOutcome.Proposal>(process(rules, ai = state,
                device = available.copy(memory = DeviceResourceState.CONSTRAINED)))
        }
    }

    @Test
    fun heavyWorkNeverRunsWithoutReadyCapabilityAndAllAvailableResources() {
        var calls = 0
        val heavy = adapter(true) { calls++; rules.extract(it) }
        LocalAiState.entries.filter { it != LocalAiState.READY }.forEach { state ->
            assertIs<NotificationExtractionOutcome.Proposal>(process(heavy, ai = state))
        }
        listOf(
            available.copy(memory = DeviceResourceState.CONSTRAINED),
            available.copy(battery = DeviceResourceState.CONSTRAINED),
            available.copy(thermal = DeviceResourceState.CONSTRAINED),
            available.copy(memory = DeviceResourceState.UNKNOWN),
            available.copy(battery = DeviceResourceState.UNKNOWN),
            available.copy(thermal = DeviceResourceState.UNKNOWN),
        ).forEach { assertIs<NotificationExtractionOutcome.Proposal>(process(heavy, device = it)) }
        assertEquals(0, calls)
        assertIs<NotificationExtractionOutcome.Proposal>(process(heavy))
        assertEquals(1, calls)
    }

    @Test
    fun unsupportedFallbackReturnsExplicitUnavailableDeferredOrFailedOutcome() {
        val input = notification.copy(content = NotificationContent("Synthetic message", null))
        val heavy = adapter(true) { error("Heavy work must not run") }
        listOf(LocalAiState.DOWNLOAD_REQUIRED, LocalAiState.DOWNLOADING,
            LocalAiState.UNAVAILABLE, LocalAiState.UNSUPPORTED).forEach {
            assertEquals(review(ExtractionReviewReason.LOCAL_AI_UNAVAILABLE),
                process(heavy, input = input, ai = it))
        }
        assertEquals(NotificationExtractionOutcome.Deferred(
            ExtractionDeferralReason.DEVICE_CONSTRAINED), process(heavy, input = input,
            device = available.copy(thermal = DeviceResourceState.UNKNOWN)))
        assertEquals(NotificationExtractionOutcome.Deferred(
            ExtractionDeferralReason.LOCAL_AI_DEFERRED),
            process(heavy, input = input, ai = LocalAiState.DEFERRED))
        assertEquals(NotificationExtractionOutcome.Failed, process(heavy, input = input,
            ai = LocalAiState.FAILED))
    }

    @Test
    fun extractorFailuresUseRulesWhenPossibleButCancellationPropagates() {
        val failed = adapter(true) { error("Synthetic adapter failure") }
        assertIs<NotificationExtractionOutcome.Proposal>(process(failed))
        assertEquals(NotificationExtractionOutcome.Failed, process(failed,
            input = notification.copy(content = NotificationContent("Synthetic message", null))))
        assertEquals(NotificationExtractionOutcome.Failed,
            process(adapter(false) { ExtractionResult.Failed }))
        assertFailsWith<CancellationException> {
            process(adapter(true) { throw CancellationException() })
        }
    }

    @Test
    fun inputLimitsUnknownAndUnsupportedGrammarRemainExplicit() {
        assertEquals(review(ExtractionReviewReason.INPUT_LIMIT), process(rules,
            input = notification.copy(content = NotificationContent("a".repeat(257), null))))
        assertEquals(review(ExtractionReviewReason.UNSUPPORTED_FORMAT), process(rules,
            input = notification.copy(content = NotificationContent("Synthetic message", null))))
        assertEquals(review(ExtractionReviewReason.UNSUPPORTED_FORMAT), process(rules,
            input = notification.copy(content = NotificationContent("Event: Synthetic",
                "Timezone: UTC"))))
        assertEquals(NotificationExtractionOutcome.Ignored, process(rules,
            input = notification.copy(content = NotificationContent(null, null))))
    }

    @Test
    fun captureAvailabilityGateKeepsContentUnreadUntilStructuredStorageIsAvailable() {
        val settings = configuration().asCaptureSettings()
        val coordinator = NotificationCaptureCoordinator({ settings },
            NotificationCaptureSink { error("No durable destination") }, { false })
        coordinator.updateConnection(true, true)
        assertEquals(CaptureResult.PROCESSOR_UNAVAILABLE, coordinator.capture(source, 100) {
            error("Content must not be read")
        })
        assertEquals(false, NotificationCaptureSettings(emptySet(), null, false).admits(source))
    }

    @Test
    fun confidenceAndExpiryRemainExplicitValidatedCallerPolicies() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, -0.1, 1.1).forEach {
            assertFailsWith<IllegalArgumentException> { ExtractionAssessment(it, false) }
        }
        assertFailsWith<IllegalArgumentException> {
            ExtractionProposalRequest("p", "calendar", "Synthetic", 101, 101)
        }
        val result = assertIs<NotificationExtractionOutcome.Proposal>(process(rules,
            proposalRequest = ExtractionProposalRequest("p", "calendar", "Synthetic", 101, 200)))
        assertEquals(200L, result.proposal.expiresAtEpochMilliseconds)
    }

    private fun process(
        extractor: NotificationExtractor,
        config: LocalConfiguration? = configuration(),
        input: ObservedNotification = notification,
        confidence: ExtractionAssessment = assessment,
        device: ExtractionDeviceConditions = available,
        ai: LocalAiState = LocalAiState.READY,
        proposalRequest: ExtractionProposalRequest = request,
    ): NotificationExtractionOutcome = NotificationExtractionPipeline(extractor, rules, policy)
        .process(input, config, proposalRequest, confidence, device, ai)

    private fun configuration(
        enabled: Boolean = true,
        action: AutomationAction = AutomationAction.PROPOSE_CALENDAR_EVENT,
        exclusions: Set<NotificationClassification> = emptySet(),
    ): LocalConfiguration = LocalConfiguration(AutomationProfiles(listOf(
        AutomationProfile("personal", "Personal", automations = listOf(Automation("calendar",
            "Calendar", enabled, NotificationSourceSelector.AnyMonitoredApplication, action,
            exclusions = exclusions))),
        AutomationProfile("other", "Other", automations = listOf(Automation("other-calendar",
            "Other", true, NotificationSourceSelector.Application("other.app"), action))),
    ), "personal"), setOf(source.applicationId), false)

    private fun adapter(heavy: Boolean, extract: (ObservedNotification) -> ExtractionResult):
        NotificationExtractor = object : NotificationExtractor {
            override val requiresHeavyWork: Boolean = heavy
            override fun extract(notification: ObservedNotification): ExtractionResult =
                extract(notification)
        }

    private fun review(reason: ExtractionReviewReason): NotificationExtractionOutcome =
        NotificationExtractionOutcome.ReviewNeeded(reason)
}
