package com.hayduck.notemate.notification

import com.hayduck.notemate.domain.configuration.LocalConfiguration
import com.hayduck.notemate.domain.notification.CaptureResult
import com.hayduck.notemate.domain.notification.NotificationCaptureCoordinator
import com.hayduck.notemate.domain.notification.NotificationCaptureSink
import com.hayduck.notemate.domain.notification.NotificationSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class CaptureSettingsObserverTest {
    @Test
    fun loadingFailureAndPauseDenySourceAdmission() = runTest {
        val configurations = MutableSharedFlow<LocalConfiguration>()
        val observer = CaptureSettingsObserver(configurations, backgroundScope)
        val source = NotificationSource("example.synthetic")
        assertFalse(observer.settings.value.admits(source))
        runCurrent()

        val enabled = LocalConfiguration.initial()
            .withMonitoredApplications(setOf(source.applicationId))
        configurations.emit(enabled)
        runCurrent()
        assertTrue(observer.settings.value.admits(source))
        configurations.emit(enabled.withPaused(true))
        runCurrent()
        assertFalse(observer.settings.value.admits(source))
        assertEquals("personal", observer.settings.value.profiles?.selectedProfileId)
    }

    @Test
    fun failedStorageRemovesPreviouslyLoadedSettings() = runTest {
        val updates = MutableSharedFlow<Boolean>()
        val observer = CaptureSettingsObserver(updates.map { isAvailable ->
            check(isAvailable) { "Synthetic storage failure." }
            LocalConfiguration.initial().withMonitoredApplications(setOf("example.synthetic"))
        }, backgroundScope)
        runCurrent()
        updates.emit(true)
        runCurrent()
        assertTrue(observer.settings.value.admits(NotificationSource("example.synthetic")))
        updates.emit(false)
        runCurrent()
        assertFalse(observer.settings.value.admits(NotificationSource("example.synthetic")))
        assertEquals(null, observer.settings.value.profiles)
    }

    @Test
    fun loadedConfigurationDoesNotEnableContentAccessWithoutProcessor() = runTest {
        val observer = CaptureSettingsObserver(flow {
            emit(LocalConfiguration.initial()
                .withMonitoredApplications(setOf("example.synthetic")))
        }, backgroundScope)
        runCurrent()
        val coordinator = NotificationCaptureCoordinator(
            settings = { observer.settings.value },
            sink = NotificationCaptureSink { error("Processor must remain unavailable.") },
            isProcessorAvailable = { false },
        )
        coordinator.updateConnection(isConnected = true, hasAccess = true)
        assertEquals(CaptureResult.PROCESSOR_UNAVAILABLE, coordinator.capture(
            NotificationSource("example.synthetic"), 1L,
        ) { error("Content must not be read.") })
    }
}
