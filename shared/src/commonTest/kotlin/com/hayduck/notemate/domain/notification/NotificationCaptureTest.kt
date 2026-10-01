package com.hayduck.notemate.domain.notification

import com.hayduck.notemate.domain.automation.Automation
import com.hayduck.notemate.domain.automation.AutomationAction
import com.hayduck.notemate.domain.automation.AutomationProfile
import com.hayduck.notemate.domain.automation.AutomationProfiles
import com.hayduck.notemate.domain.automation.NotificationSourceSelector
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationCaptureTest {
    private val source = NotificationSource("synthetic.app")

    @Test
    fun containsPayloadFailuresButPropagatesCancellation() {
        val coordinator = NotificationCaptureCoordinator(
            { NotificationCaptureSettings(setOf(source.applicationId), profiles(), false) },
            NotificationCaptureSink { CaptureResult.ACCEPTED },
        )
        coordinator.updateConnection(true, true)
        assertEquals(CaptureResult.FAILED, coordinator.capture(source, 1) {
            error("Synthetic mapping failure")
        })
        assertEquals(CaptureResult.FAILED, coordinator.lastCaptureResult)
        assertFailsWith<CancellationException> {
            coordinator.capture(source, 1) { throw CancellationException() }
        }
    }

    @Test
    fun deniedSourcesAndUnavailableProcessorNeverReadContent() {
        var settings = NotificationCaptureSettings(emptySet(), profiles(), false)
        var available = true
        val coordinator = NotificationCaptureCoordinator(
            { settings }, NotificationCaptureSink { error("Sink must not run") }, { available },
        )
        coordinator.updateConnection(true, true)
        assertEquals(CaptureResult.REJECTED, coordinator.capture(source, 1) { error("Private read") })
        settings = NotificationCaptureSettings(setOf(source.applicationId), profiles(), false)
        available = false
        assertEquals(
            CaptureResult.PROCESSOR_UNAVAILABLE,
            coordinator.capture(source, 1) { error("Private read") },
        )
    }

    @Test
    fun captureUsesCurrentSelectedProfileAndPauseAndAccess() {
        var settings = NotificationCaptureSettings(setOf(source.applicationId), profiles(), false)
        var count = 0
        val coordinator = NotificationCaptureCoordinator(
            { settings }, NotificationCaptureSink { count++; CaptureResult.ACCEPTED },
        )
        coordinator.updateConnection(true, true)
        assertEquals(CaptureResult.ACCEPTED, coordinator.capture(source, 1) {
            NotificationContent("Synthetic title", "Synthetic event")
        })
        settings = NotificationCaptureSettings(setOf(source.applicationId), profiles(), true)
        assertEquals(CaptureResult.REJECTED, coordinator.capture(source, 1) { error("Paused read") })
        settings = NotificationCaptureSettings(setOf(source.applicationId), profiles("other"), false)
        assertEquals(CaptureResult.REJECTED, coordinator.capture(source, 1) { error("Profile read") })
        coordinator.updateConnection(false, false)
        assertEquals(ListenerConnectionState.ACCESS_REVOKED, coordinator.connectionState)
        assertEquals(CaptureResult.REJECTED, coordinator.capture(source, 1) { error("Revoked read") })
        coordinator.updateConnection(false, true)
        assertEquals(ListenerConnectionState.DISCONNECTED, coordinator.connectionState)
        assertEquals(1, count)
    }

    @Test
    fun unloadedDisabledAndMutatedCallerSettingsFailClosed() {
        val ids = mutableSetOf(source.applicationId)
        val settings = NotificationCaptureSettings(ids, profiles(), false)
        ids.clear()
        assertTrue(settings.admits(source))
        assertFalse(NotificationCaptureSettings(ids, null, false).admits(source))
        assertFalse(NotificationCaptureSettings(setOf(source.applicationId), profiles(enabled = false),
            false).admits(source))
    }

    private fun profiles(selected: String = "personal", enabled: Boolean = true): AutomationProfiles =
        AutomationProfiles(listOf(
            AutomationProfile("personal", "Personal", automations = listOf(Automation(
                "calendar", "Calendar", enabled, NotificationSourceSelector.AnyMonitoredApplication,
                AutomationAction.PROPOSE_CALENDAR_EVENT,
            ))),
            AutomationProfile("other", "Other", automations = listOf(Automation(
                "other-calendar", "Other Calendar", true,
                NotificationSourceSelector.Application("other.app"),
                AutomationAction.PROPOSE_CALENDAR_EVENT,
            ))),
        ), selected)
}
