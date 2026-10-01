package com.hayduck.notemate.notification

import com.hayduck.notemate.domain.configuration.LocalConfiguration
import com.hayduck.notemate.domain.notification.NotificationCaptureSettings
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Fails closed before loading or after a storage failure; never substitutes starter settings. */
internal class CaptureSettingsObserver(
    configuration: Flow<LocalConfiguration>,
    scope: CoroutineScope,
) {
    private val unloaded = NotificationCaptureSettings(emptySet(), null, isPaused = false)
    val settings: StateFlow<NotificationCaptureSettings> = configuration
        .map { it.asCaptureSettings() }
        .catch { failure ->
            if (failure is CancellationException) throw failure
            emit(unloaded)
        }
        .stateIn(scope, SharingStarted.Eagerly, unloaded)
}
