package com.hayduck.notemate.today

import com.hayduck.notemate.domain.activity.ActivityRecord
import com.hayduck.notemate.domain.automation.Automation
import com.hayduck.notemate.domain.automation.AutomationProfile
import com.hayduck.notemate.domain.configuration.LocalConfiguration
import com.hayduck.notemate.domain.configuration.LocalConfigurationRepository
import com.hayduck.notemate.domain.proposal.CalendarDate
import com.hayduck.notemate.domain.proposal.CalendarFields
import com.hayduck.notemate.domain.proposal.CalendarProposal
import com.hayduck.notemate.domain.proposal.CalendarTime
import com.hayduck.notemate.domain.proposal.ProposalOrigin
import com.hayduck.notemate.domain.proposal.ProposalState
import com.hayduck.notemate.domain.results.LocalResults
import com.hayduck.notemate.domain.results.LocalResultsRepository
import com.hayduck.notemate.domain.results.ProposalChange
import com.hayduck.notemate.domain.results.StoredCalendarProposal
import com.hayduck.notemate.domain.results.asNewStoredProposal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class TodayStateHolderTest {
    @Test
    fun educationProgressSurvivesRestartAndSettingsNeverAssumeAccessWasGranted() = runTest {
        val fixture = fixture()
        val effects = mutableListOf<TodayEffect>()
        backgroundScope.launch { fixture.holder.effects.collect { effects += it } }
        runCurrent()
        assertEquals(TodayDestination.WELCOME, fixture.holder.state.value.destination)
        fixture.holder.onAction(TodayAction.START_SETUP)
        runCurrent()
        assertEquals(OnboardingStep.PRIVACY, fixture.store.preferences.step)
        fixture.holder.onAction(TodayAction.CONTINUE_PRIVACY)
        runCurrent()
        assertEquals(OnboardingStep.ACCESS, fixture.store.preferences.step)
        fixture.holder.onAction(TodayAction.OPEN_ACCESS_SETTINGS)
        runCurrent()
        assertEquals(listOf(TodayEffect.OPEN_ACCESS_SETTINGS), effects)
        fixture.holder.updatePermissions(
            PermissionSnapshot(false, ProposalAlertPermission.NOT_REQUESTED),
        )
        fixture.holder.onAction(TodayAction.CONTINUE_ACCESS)
        runCurrent()
        assertEquals(TodayDestination.ACCESS, fixture.holder.state.value.destination)
        val restarted = fixture(store = fixture.store)
        runCurrent()
        assertEquals(TodayDestination.ACCESS, restarted.holder.state.value.destination)
        restarted.holder.updatePermissions(
            PermissionSnapshot(true, ProposalAlertPermission.NOT_REQUESTED),
        )
        runCurrent()
        restarted.holder.onAction(TodayAction.CONTINUE_ACCESS)
        runCurrent()
        assertEquals(TodayDestination.ALERTS, restarted.holder.state.value.destination)
        assertTrue(restarted.store.preferences.hasHadAccess)
        assertEquals(TodayProcessingState.PROCESSOR_UNAVAILABLE,
            restarted.holder.state.value.processing)
    }

    @Test
    fun skippingSetupOpensInactiveTodayWithoutInventingAccessAndPersistsCompletion() = runTest {
        val fixture = fixture(store = MemoryOnboardingStore(OnboardingStep.ACCESS))
        runCurrent()
        fixture.holder.updatePermissions(
            PermissionSnapshot(false, ProposalAlertPermission.NOT_REQUESTED),
        )
        fixture.holder.onAction(TodayAction.SKIP_SETUP)
        runCurrent()
        assertTrue(fixture.store.preferences.completed)
        assertEquals(TodayDestination.TODAY, fixture.holder.state.value.destination)
        assertEquals(TodayProcessingState.PERMISSION_REQUIRED,
            fixture.holder.state.value.processing)
        assertFalse(fixture.store.preferences.hasHadAccess)
        val restarted = fixture(store = fixture.store)
        runCurrent()
        assertEquals(TodayDestination.TODAY, restarted.holder.state.value.destination)
    }

    @Test
    fun grantThenRevocationPreservesStoredResultsAndHistoryAcrossRestart() = runTest {
        val results = LocalResults(listOf(syntheticProposal()), emptyList())
        val fixture = fixture(results = MemoryResultsRepository(results))
        runCurrent()
        fixture.holder.updatePermissions(PermissionSnapshot(true, ProposalAlertPermission.GRANTED))
        runCurrent()
        assertEquals(NotificationAccessState.GRANTED, fixture.holder.state.value.access)
        fixture.holder.updatePermissions(PermissionSnapshot(false, ProposalAlertPermission.GRANTED))
        runCurrent()
        assertEquals(NotificationAccessState.REVOKED, fixture.holder.state.value.access)
        assertEquals("synthetic-proposal", fixture.holder.readyResults().proposals.single().id)
        val restarted = fixture(store = fixture.store, results = fixture.results)
        runCurrent()
        restarted.holder.updatePermissions(
            PermissionSnapshot(false, ProposalAlertPermission.DENIED),
        )
        runCurrent()
        assertEquals(NotificationAccessState.REVOKED, restarted.holder.state.value.access)
        assertEquals(1, restarted.holder.readyResults().proposals.size)
        assertSame(results, fixture.results.values.value)
        assertEquals(0, fixture.results.mutations)
    }

    @Test
    fun alertRequestIsExplicitAndDenialOnlyChangesAlertState() = runTest {
        val fixture = fixture(
            store = MemoryOnboardingStore(OnboardingStep.ALERTS),
            results = MemoryResultsRepository(
                LocalResults(listOf(syntheticProposal()), emptyList()),
            ),
        )
        val effects = mutableListOf<TodayEffect>()
        backgroundScope.launch { fixture.holder.effects.collect { effects += it } }
        runCurrent()
        fixture.holder.updatePermissions(
            PermissionSnapshot(true, ProposalAlertPermission.NOT_REQUESTED),
        )
        runCurrent()
        assertTrue(effects.isEmpty())
        fixture.holder.onAction(TodayAction.ENABLE_ALERTS)
        runCurrent()
        assertEquals(listOf(TodayEffect.REQUEST_ALERT_PERMISSION), effects)
        assertTrue(fixture.holder.hasRequestedAlerts)
        fixture.holder.updatePermissions(PermissionSnapshot(true, ProposalAlertPermission.DENIED))
        runCurrent()
        assertEquals(TodayProcessingState.PROCESSOR_UNAVAILABLE,
            fixture.holder.state.value.processing)
        assertEquals(1, fixture.holder.readyResults().proposals.size)
        fixture.holder.onAction(TodayAction.ENABLE_ALERTS)
        runCurrent()
        assertEquals(
            listOf(TodayEffect.REQUEST_ALERT_PERMISSION, TodayEffect.OPEN_ALERT_SETTINGS),
            effects,
        )
        fixture.holder.updatePermissions(
            PermissionSnapshot(true, ProposalAlertPermission.CHANNEL_DISABLED),
        )
        runCurrent()
        assertEquals(TodayProcessingState.PROCESSOR_UNAVAILABLE,
            fixture.holder.state.value.processing)
        assertEquals(0, fixture.results.mutations)
    }

    @Test
    fun configurationAndResultsFailuresRetryIndependentlyWithoutResettingStorage() = runTest {
        val configuration = MemoryConfigurationRepository().apply { failObservation = true }
        val results = MemoryResultsRepository(
            LocalResults(listOf(syntheticProposal()), emptyList()),
        )
        val fixture = fixture(configuration = configuration, results = results)
        runCurrent()
        assertIs<TodayConfigurationUiState.Failed>(fixture.holder.state.value.configuration)
        assertEquals(1, fixture.holder.readyResults().proposals.size)
        configuration.failObservation = false
        fixture.holder.onAction(TodayAction.RETRY_CONFIGURATION)
        runCurrent()
        assertIs<TodayConfigurationUiState.Ready>(fixture.holder.state.value.configuration)
        results.failObservation = true
        fixture.holder.onAction(TodayAction.RETRY_RESULTS)
        runCurrent()
        assertIs<TodayResultsUiState.Failed>(fixture.holder.state.value.results)
        assertIs<TodayConfigurationUiState.Ready>(fixture.holder.state.value.configuration)
        results.failObservation = false
        fixture.holder.onAction(TodayAction.RETRY_RESULTS)
        runCurrent()
        assertEquals(1, fixture.holder.readyResults().proposals.size)
        assertEquals(0, results.mutations)
    }

    @Test
    fun pauseResumeOnlyChangesGlobalPauseAndKeepsProfileAndResults() = runTest {
        val fixture = fixture(
            store = MemoryOnboardingStore(OnboardingStep.COMPLETE),
            results = MemoryResultsRepository(
                LocalResults(listOf(syntheticProposal()), emptyList()),
            ),
        )
        runCurrent()
        fixture.holder.updatePermissions(PermissionSnapshot(true, ProposalAlertPermission.GRANTED))
        runCurrent()
        val selected = fixture.configuration.values.value.profiles.selectedProfile
        val results = fixture.results.values.value
        fixture.holder.onAction(TodayAction.PAUSE)
        runCurrent()
        assertEquals(TodayProcessingState.PAUSED, fixture.holder.state.value.processing)
        assertSame(selected, fixture.configuration.values.value.profiles.selectedProfile)
        fixture.holder.onAction(TodayAction.RESUME)
        runCurrent()
        assertEquals(TodayProcessingState.PROCESSOR_UNAVAILABLE,
            fixture.holder.state.value.processing)
        assertSame(selected, fixture.configuration.values.value.profiles.selectedProfile)
        assertSame(results, fixture.results.values.value)
        assertEquals(listOf(true, false), fixture.configuration.pauseChanges)
        assertEquals(0, fixture.results.mutations)
    }

    @Test
    fun expiryUpdatesPresentationAtDeadlineWithoutWritingProposalState() = runTest {
        val proposal = syntheticProposal(expiresAt = 2_000)
        val results = MemoryResultsRepository(LocalResults(listOf(proposal), emptyList()))
        val fixture = fixture(results = results)
        runCurrent()
        assertEquals(ProposalState.READY, fixture.holder.readyResults().proposals.single().state)
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(ProposalState.EXPIRED, fixture.holder.readyResults().proposals.single().state)
        assertEquals(ProposalState.READY, results.values.value.proposals.single().proposal.state)
        assertEquals(0, results.mutations)
    }

    @Test
    fun failedProgressSaveBlocksNavigationAndAlertRequestUntilRetrySucceeds() = runTest {
        val store = MemoryOnboardingStore().apply { failSave = true }
        val fixture = fixture(store = store)
        runCurrent()
        fixture.holder.updatePermissions(
            PermissionSnapshot(false, ProposalAlertPermission.NOT_REQUESTED),
        )
        fixture.holder.onAction(TodayAction.START_SETUP)
        runCurrent()
        assertEquals(TodayMessage.SAVE_FAILED, fixture.holder.state.value.message)
        assertEquals(TodayDestination.WELCOME, fixture.holder.state.value.destination)
        val alertStore = MemoryOnboardingStore(OnboardingStep.ALERTS).apply { failSave = true }
        val alerts = fixture(store = alertStore)
        val effects = mutableListOf<TodayEffect>()
        backgroundScope.launch { alerts.holder.effects.collect { effects += it } }
        runCurrent()
        alerts.holder.updatePermissions(
            PermissionSnapshot(false, ProposalAlertPermission.NOT_REQUESTED),
        )
        alerts.holder.onAction(TodayAction.ENABLE_ALERTS)
        runCurrent()
        assertTrue(effects.isEmpty())
        assertFalse(alerts.holder.hasRequestedAlerts)
        assertEquals(TodayMessage.SAVE_FAILED, alerts.holder.state.value.message)
        alertStore.failSave = false
        alerts.holder.onAction(TodayAction.ENABLE_ALERTS)
        runCurrent()
        assertEquals(listOf(TodayEffect.REQUEST_ALERT_PERMISSION), effects)
        assertTrue(alerts.holder.hasRequestedAlerts)
    }

    @Test
    fun platformEffectsRequireTheirOwnEducationDestination() = runTest {
        OnboardingStep.entries.forEach { step ->
            val fixture = fixture(store = MemoryOnboardingStore(step))
            val effects = mutableListOf<TodayEffect>()
            backgroundScope.launch { fixture.holder.effects.collect { effects += it } }
            runCurrent()
            fixture.holder.updatePermissions(
                PermissionSnapshot(false, ProposalAlertPermission.NOT_REQUESTED),
            )
            if (step != OnboardingStep.ACCESS) {
                fixture.holder.onAction(TodayAction.OPEN_ACCESS_SETTINGS)
                runCurrent()
                assertTrue(effects.isEmpty())
            }
            if (step != OnboardingStep.ALERTS) {
                fixture.holder.onAction(TodayAction.ENABLE_ALERTS)
                runCurrent()
                assertTrue(effects.isEmpty())
                assertFalse(fixture.holder.hasRequestedAlerts)
            }
        }
    }

    @Test
    fun platformLaunchCannotRepeatUntilPermissionFactsAreRefreshed() = runTest {
        val fixture = fixture(store = MemoryOnboardingStore(OnboardingStep.ACCESS))
        val effects = mutableListOf<TodayEffect>()
        backgroundScope.launch { fixture.holder.effects.collect { effects += it } }
        runCurrent()
        val snapshot = PermissionSnapshot(false, ProposalAlertPermission.NOT_REQUESTED)
        fixture.holder.updatePermissions(snapshot)
        fixture.holder.onAction(TodayAction.OPEN_ACCESS_SETTINGS)
        runCurrent()
        fixture.holder.onAction(TodayAction.OPEN_ACCESS_SETTINGS)
        runCurrent()
        assertEquals(listOf(TodayEffect.OPEN_ACCESS_SETTINGS), effects)
        assertTrue(fixture.holder.state.value.isSaving)
        fixture.holder.updatePermissions(snapshot)
        runCurrent()
        assertFalse(fixture.holder.state.value.isSaving)
        fixture.holder.onAction(TodayAction.OPEN_ACCESS_SETTINGS)
        runCurrent()
        assertEquals(
            listOf(TodayEffect.OPEN_ACCESS_SETTINGS, TodayEffect.OPEN_ACCESS_SETTINGS), effects,
        )
    }

    @Test
    fun permissionsDestinationSupportsBothHandoffsAndFailedLaunchCanRecover() = runTest {
        val fixture = fixture(store = MemoryOnboardingStore(OnboardingStep.COMPLETE))
        val effects = mutableListOf<TodayEffect>()
        backgroundScope.launch { fixture.holder.effects.collect { effects += it } }
        runCurrent()
        fixture.holder.onAction(TodayAction.OPEN_PERMISSIONS)
        fixture.holder.updatePermissions(
            PermissionSnapshot(false, ProposalAlertPermission.APP_DISABLED),
        )
        fixture.holder.onAction(TodayAction.OPEN_ACCESS_SETTINGS)
        runCurrent()
        assertEquals(listOf(TodayEffect.OPEN_ACCESS_SETTINGS), effects)
        fixture.holder.settingsUnavailable()
        assertEquals(TodayMessage.SETTINGS_UNAVAILABLE, fixture.holder.state.value.message)
        assertFalse(fixture.holder.state.value.isSaving)
        fixture.holder.onAction(TodayAction.ENABLE_ALERTS)
        runCurrent()
        assertEquals(
            listOf(TodayEffect.OPEN_ACCESS_SETTINGS, TodayEffect.OPEN_ALERT_SETTINGS), effects,
        )
        assertEquals(TodayDestination.PERMISSIONS, fixture.holder.state.value.destination)
    }

    @Test
    fun onboardingLoadFailureCanRetryWithoutHidingOtherRepositoryState() = runTest {
        val store = MemoryOnboardingStore().apply { failLoad = true }
        val fixture = fixture(store = store)
        runCurrent()
        assertEquals(OnboardingLoadState.FAILED, fixture.holder.state.value.onboarding)
        assertIs<TodayConfigurationUiState.Ready>(fixture.holder.state.value.configuration)
        assertIs<TodayResultsUiState.Ready>(fixture.holder.state.value.results)
        fixture.holder.onAction(TodayAction.START_SETUP)
        runCurrent()
        assertEquals(OnboardingStep.WELCOME, store.preferences.step)
        store.failLoad = false
        fixture.holder.onAction(TodayAction.RETRY_ONBOARDING)
        runCurrent()
        assertEquals(OnboardingLoadState.READY, fixture.holder.state.value.onboarding)
        fixture.holder.onAction(TodayAction.START_SETUP)
        runCurrent()
        assertEquals(OnboardingStep.PRIVACY, store.preferences.step)
    }

    private fun TestScope.fixture(
        store: MemoryOnboardingStore = MemoryOnboardingStore(),
        configuration: MemoryConfigurationRepository = MemoryConfigurationRepository(),
        results: MemoryResultsRepository = MemoryResultsRepository(),
    ): Fixture = Fixture(
        TodayStateHolder(configuration, results, store, backgroundScope) {
            1_000 + testScheduler.currentTime
        },
        store, configuration, results,
    )

    private fun TodayStateHolder.readyResults(): TodayResultsUiState.Ready =
        assertIs<TodayResultsUiState.Ready>(state.value.results)

    private class Fixture(
        val holder: TodayStateHolder,
        val store: MemoryOnboardingStore,
        val configuration: MemoryConfigurationRepository,
        val results: MemoryResultsRepository,
    )
}

private class MemoryOnboardingStore(
    step: OnboardingStep = OnboardingStep.WELCOME,
) : OnboardingStore {
    var preferences = OnboardingPreferences(step)
    var failLoad = false
    var failSave = false

    override suspend fun load(): OnboardingPreferences {
        check(!failLoad) { "Synthetic storage failure" }
        return preferences
    }

    override suspend fun save(preferences: OnboardingPreferences) {
        check(!failSave) { "Synthetic storage failure" }
        this.preferences = preferences
    }
}

private class MemoryConfigurationRepository : LocalConfigurationRepository {
    val values = MutableStateFlow(LocalConfiguration.initial())
    var failObservation = false
    val pauseChanges = mutableListOf<Boolean>()
    override val configuration: Flow<LocalConfiguration> get() = flow {
        check(!failObservation) { "Synthetic storage failure" }
        emitAll(values)
    }

    override suspend fun getConfiguration(): LocalConfiguration = values.value
    override suspend fun setPaused(isPaused: Boolean) {
        pauseChanges += isPaused
        values.value = values.value.withPaused(isPaused)
    }

    override suspend fun setMonitoredApplications(applicationIds: Set<String>) = unexpected()
    override suspend fun selectProfile(profileId: String) = unexpected()
    override suspend fun saveProfile(profile: AutomationProfile) = unexpected()
    override suspend fun deleteProfile(
        profileId: String,
        replacementProfileId: String?,
    ) = unexpected()
    override suspend fun saveAutomation(profileId: String, automation: Automation) = unexpected()
    override suspend fun deleteAutomation(profileId: String, automationId: String) = unexpected()
    private fun unexpected(): Nothing = error("Unexpected configuration mutation")
}

private class MemoryResultsRepository(
    initial: LocalResults = LocalResults(emptyList(), emptyList()),
) : LocalResultsRepository {
    val values = MutableStateFlow(initial)
    var failObservation = false
    var mutations = 0
        private set
    override val results: Flow<LocalResults> get() = flow {
        check(!failObservation) { "Synthetic storage failure" }
        emitAll(values)
    }

    override suspend fun get(): LocalResults = values.value
    override suspend fun createProposal(
        proposal: CalendarProposal,
        activityId: String,
    ): StoredCalendarProposal = unexpected()

    override suspend fun changeProposal(
        proposalId: String,
        expectedRevision: Long,
        change: ProposalChange,
        activityId: String,
        nowEpochMilliseconds: Long,
    ): StoredCalendarProposal = unexpected()

    override suspend fun appendActivity(activity: ActivityRecord) = unexpected()
    override suspend fun clearActivity() = unexpected()
    override suspend fun deleteProposal(proposalId: String, expectedRevision: Long) = unexpected()
    private fun unexpected(): Nothing {
        mutations++
        error("Unexpected results mutation")
    }
}

internal fun syntheticProposal(
    id: String = "synthetic-proposal",
    createdAt: Long = 100,
    expiresAt: Long? = null,
): StoredCalendarProposal = CalendarProposal.draft(
    id = id,
    origin = ProposalOrigin(
        "synthetic-profile", "Synthetic Profile", "synthetic-automation", "Synthetic Automation",
        "synthetic.application", "Synthetic Application",
    ),
    fields = CalendarFields("Synthetic event", CalendarDate(2027, 1, 2), CalendarTime(18, 5)),
    createdAtEpochMilliseconds = createdAt,
    expiresAtEpochMilliseconds = expiresAt,
).validate(createdAt).asNewStoredProposal()
