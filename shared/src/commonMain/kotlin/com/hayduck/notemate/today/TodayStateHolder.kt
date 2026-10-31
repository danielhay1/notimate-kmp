package com.hayduck.notemate.today

import com.hayduck.notemate.domain.activity.ActivityRecord
import com.hayduck.notemate.domain.configuration.LocalConfigurationRepository
import com.hayduck.notemate.domain.proposal.CalendarProposal
import com.hayduck.notemate.domain.proposal.ProposalState
import com.hayduck.notemate.domain.results.LocalResults
import com.hayduck.notemate.domain.results.LocalResultsRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coordinates local screen truth without enabling processing or mutating proposal lifecycle.
 * The owner supplies a main-thread scope and cancels it when the retained screen owner is cleared.
 */
class TodayStateHolder(
    private val configurationRepository: LocalConfigurationRepository,
    private val resultsRepository: LocalResultsRepository,
    private val onboardingStore: OnboardingStore,
    private val scope: CoroutineScope,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val mutableState = MutableStateFlow(TodayUiState())
    val state = mutableState.asStateFlow()
    private val effectChannel = Channel<TodayEffect>(Channel.BUFFERED)
    val effects = effectChannel.receiveAsFlow()

    private var preferences: OnboardingPreferences? = null
    private var permissions: PermissionSnapshot? = null
    private var storedResults: LocalResults? = null
    private val preferenceMutex = Mutex()
    private var configurationJob: Job? = null
    private var resultsJob: Job? = null
    private var onboardingJob: Job? = null
    private var expiryJob: Job? = null
    private var actionJob: Job? = null
    private var historyJob: Job? = null
    private var platformEffectPending = false

    val hasRequestedAlerts: Boolean get() = preferences?.hasRequestedAlerts == true

    init {
        loadOnboarding()
        observeConfiguration()
        observeResults()
    }

    /** Refresh from OS facts on foreground entry and after every platform result. */
    fun updatePermissions(snapshot: PermissionSnapshot) {
        permissions = snapshot
        platformEffectPending = false
        mutableState.value = mutableState.value.copy(
            access = accessState(),
            alerts = snapshot.alerts,
            isSaving = actionJob?.isActive == true,
        )
        rememberGrantedAccess()
        projectResults()
    }

    fun settingsUnavailable() {
        platformEffectPending = false
        mutableState.value = mutableState.value.copy(
            isSaving = false,
            message = TodayMessage.SETTINGS_UNAVAILABLE,
        )
    }

    fun permissionCheckFailed() {
        permissions = null
        platformEffectPending = false
        mutableState.value = mutableState.value.copy(
            access = NotificationAccessState.CHECKING,
            isSaving = false,
            message = TodayMessage.PERMISSION_CHECK_FAILED,
        )
    }

    fun onAction(action: TodayAction) {
        if (!actionIsAvailable(action)) return
        when (action) {
            TodayAction.RETRY_ONBOARDING -> loadOnboarding()
            TodayAction.RETRY_CONFIGURATION -> observeConfiguration()
            TodayAction.RETRY_RESULTS -> observeResults()
            TodayAction.DISMISS_MESSAGE ->
                mutableState.value = mutableState.value.copy(message = null)
            TodayAction.OPEN_PERMISSIONS -> navigate(TodayDestination.PERMISSIONS)
            TodayAction.BACK_TO_TODAY -> navigate(TodayDestination.TODAY)
            else -> {
                if (mutableState.value.onboarding != OnboardingLoadState.READY ||
                    mutableState.value.isSaving
                ) return
                mutableState.value = mutableState.value.copy(isSaving = true, message = null)
                actionJob = scope.launch {
                    try {
                        handleAction(action)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        mutableState.value = mutableState.value.copy(
                            message = TodayMessage.ACTION_FAILED,
                        )
                    } finally {
                        mutableState.value = mutableState.value.copy(
                            isSaving = platformEffectPending,
                        )
                    }
                }
            }
        }
    }

    private fun actionIsAvailable(action: TodayAction): Boolean {
        val destination = mutableState.value.destination
        return when (action) {
            TodayAction.START_SETUP -> destination == TodayDestination.WELCOME
            TodayAction.CONTINUE_PRIVACY -> destination == TodayDestination.PRIVACY
            TodayAction.CONTINUE_ACCESS, TodayAction.SKIP_SETUP ->
                destination == TodayDestination.ACCESS
            TodayAction.FINISH_SETUP -> destination == TodayDestination.ALERTS
            TodayAction.OPEN_ACCESS_SETTINGS -> destination in setOf(
                TodayDestination.ACCESS, TodayDestination.PERMISSIONS,
            )
            TodayAction.ENABLE_ALERTS -> destination in setOf(
                TodayDestination.ALERTS, TodayDestination.PERMISSIONS,
            )
            TodayAction.PAUSE, TodayAction.RESUME, TodayAction.OPEN_PERMISSIONS ->
                destination == TodayDestination.TODAY
            TodayAction.BACK_TO_TODAY -> destination == TodayDestination.PERMISSIONS
            TodayAction.GO_BACK -> destination in setOf(
                TodayDestination.PRIVACY, TodayDestination.ACCESS, TodayDestination.ALERTS,
            )
            else -> true
        }
    }

    private suspend fun handleAction(action: TodayAction) {
        when (action) {
            TodayAction.START_SETUP -> moveOnboarding(OnboardingStep.PRIVACY)
            TodayAction.CONTINUE_PRIVACY -> moveOnboarding(OnboardingStep.ACCESS)
            TodayAction.CONTINUE_ACCESS -> if (permissions?.hasNotificationAccess == true) {
                moveOnboarding(OnboardingStep.ALERTS)
            }
            TodayAction.SKIP_SETUP, TodayAction.FINISH_SETUP ->
                moveOnboarding(OnboardingStep.COMPLETE)
            TodayAction.GO_BACK -> moveOnboarding(when (preferences?.step) {
                OnboardingStep.PRIVACY -> OnboardingStep.WELCOME
                OnboardingStep.ACCESS -> OnboardingStep.PRIVACY
                OnboardingStep.ALERTS -> OnboardingStep.ACCESS
                else -> return
            })
            TodayAction.OPEN_ACCESS_SETTINGS -> emitEffect(TodayEffect.OPEN_ACCESS_SETTINGS)
            TodayAction.ENABLE_ALERTS -> {
                val snapshot = permissions ?: return
                if (snapshot.alerts == ProposalAlertPermission.NOT_REQUESTED) {
                    if (savePreferences { it.copy(hasRequestedAlerts = true) }) {
                        emitEffect(TodayEffect.REQUEST_ALERT_PERMISSION)
                    }
                } else {
                    emitEffect(TodayEffect.OPEN_ALERT_SETTINGS)
                }
            }
            TodayAction.PAUSE -> configurationRepository.setPaused(true)
            TodayAction.RESUME -> configurationRepository.setPaused(false)
            else -> Unit
        }
    }

    private suspend fun emitEffect(effect: TodayEffect) {
        platformEffectPending = true
        effectChannel.send(effect)
    }

    private fun navigate(destination: TodayDestination) {
        if (mutableState.value.onboarding != OnboardingLoadState.READY) return
        mutableState.value = mutableState.value.copy(destination = destination, message = null)
    }

    private suspend fun moveOnboarding(step: OnboardingStep) {
        if (savePreferences { it.copy(step = step) }) navigate(step.destination())
    }

    private fun loadOnboarding() {
        if (onboardingJob?.isActive == true) return
        mutableState.value = mutableState.value.copy(onboarding = OnboardingLoadState.LOADING)
        onboardingJob = scope.launch {
            try {
                preferences = onboardingStore.load()
                mutableState.value = mutableState.value.copy(
                    onboarding = OnboardingLoadState.READY,
                    destination = requireNotNull(preferences).step.destination(),
                    access = accessState(),
                    message = null,
                )
                rememberGrantedAccess()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(
                    onboarding = OnboardingLoadState.FAILED,
                )
            }
        }
    }

    private fun rememberGrantedAccess() {
        if (permissions?.hasNotificationAccess != true || preferences == null ||
            preferences?.hasHadAccess == true || historyJob?.isActive == true
        ) return
        historyJob = scope.launch { savePreferences { it.copy(hasHadAccess = true) } }
    }

    private suspend fun savePreferences(
        transform: (OnboardingPreferences) -> OnboardingPreferences,
    ): Boolean = preferenceMutex.withLock {
        val current = preferences ?: return@withLock false
        try {
            val next = transform(current)
            onboardingStore.save(next)
            preferences = next
            mutableState.value = mutableState.value.copy(access = accessState())
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            mutableState.value = mutableState.value.copy(message = TodayMessage.SAVE_FAILED)
            false
        }
    }

    private fun accessState(): NotificationAccessState = when {
        permissions == null -> NotificationAccessState.CHECKING
        permissions?.hasNotificationAccess == true -> NotificationAccessState.GRANTED
        preferences?.hasHadAccess == true -> NotificationAccessState.REVOKED
        else -> NotificationAccessState.NOT_GRANTED
    }

    private fun observeConfiguration() {
        configurationJob?.cancel()
        mutableState.value = mutableState.value.copy(
            configuration = TodayConfigurationUiState.Loading,
        )
        configurationJob = scope.launch {
            try {
                configurationRepository.configuration.collect { configuration ->
                    val profile = configuration.profiles.selectedProfile
                    mutableState.value = mutableState.value.copy(
                        configuration = TodayConfigurationUiState.Ready(
                            profile.name,
                            configuration.isPaused,
                            configuration.monitoredApplicationIds.size,
                            profile.automations.count { it.isEnabled },
                        ),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(
                    configuration = TodayConfigurationUiState.Failed,
                )
            }
        }
    }

    private fun observeResults() {
        resultsJob?.cancel()
        storedResults = null
        expiryJob?.cancel()
        mutableState.value = mutableState.value.copy(results = TodayResultsUiState.Loading)
        resultsJob = scope.launch {
            try {
                resultsRepository.results.collect { results ->
                    storedResults = results
                    projectResults()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                storedResults = null
                expiryJob?.cancel()
                mutableState.value = mutableState.value.copy(results = TodayResultsUiState.Failed)
            }
        }
    }

    private fun projectResults() {
        val results = storedResults ?: return
        val currentTime = now()
        mutableState.value = mutableState.value.copy(results = results.toTodayUiState(currentTime))
        expiryJob?.cancel()
        val nextExpiry = results.proposals.mapNotNull { stored ->
            stored.proposal.takeUnless {
                it.state in setOf(
                    ProposalState.DISMISSED, ProposalState.HANDED_OFF, ProposalState.EXPIRED,
                )
            }?.expiresAtEpochMilliseconds?.takeIf { it > currentTime }
        }.minOrNull() ?: return
        expiryJob = scope.launch {
            delay((nextExpiry - currentTime).coerceAtLeast(1L))
            projectResults()
        }
    }
}

private fun OnboardingStep.destination(): TodayDestination = when (this) {
    OnboardingStep.WELCOME -> TodayDestination.WELCOME
    OnboardingStep.PRIVACY -> TodayDestination.PRIVACY
    OnboardingStep.ACCESS -> TodayDestination.ACCESS
    OnboardingStep.ALERTS -> TodayDestination.ALERTS
    OnboardingStep.COMPLETE -> TodayDestination.TODAY
}

/** Presentation limits never delete stored records or synthesize audit events. */
internal fun LocalResults.toTodayUiState(now: Long): TodayResultsUiState.Ready =
    TodayResultsUiState.Ready(
        proposals = proposals.asSequence()
            .map { it.proposal }
            .filter { it.state !in setOf(ProposalState.DISMISSED, ProposalState.HANDED_OFF) }
            .sortedWith(compareByDescending<CalendarProposal> {
                it.createdAtEpochMilliseconds
            }.thenBy { it.id })
            .map { proposal ->
                val origin = proposal.origin
                TodayProposalUiState(
                    proposal.id, proposal.fields.title, proposal.fields.date, proposal.fields.time,
                    proposal.fields.location, origin.applicationName, origin.profileName,
                    origin.automationName,
                    proposal.expire(maxOf(now, proposal.createdAtEpochMilliseconds)).state,
                )
            }.toList(),
        recentOutcomes = activity.sortedWith(
            compareByDescending<ActivityRecord> {
                it.occurredAtEpochMilliseconds
            }.thenBy { it.id },
        ).take(5).map { RecentOutcomeUiState(it.id, it.event, it.occurredAtEpochMilliseconds) },
    )
