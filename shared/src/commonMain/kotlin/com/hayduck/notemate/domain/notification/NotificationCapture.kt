package com.hayduck.notemate.domain.notification

import com.hayduck.notemate.domain.automation.AutomationProfiles
import kotlin.coroutines.cancellation.CancellationException

/** Atomic local configuration; null Profiles means settings have not loaded, so capture is denied. */
class NotificationCaptureSettings(
    monitoredApplicationIds: Set<String>,
    val profiles: AutomationProfiles?,
    val isPaused: Boolean,
) {
    private val monitoredIds = monitoredApplicationIds.toSet()

    fun admits(source: NotificationSource): Boolean =
        !isPaused && source.applicationId in monitoredIds &&
            profiles?.selectedProfile?.automations?.any {
                it.isEnabled && it.sourceSelector.matches(source)
            } == true
}

enum class ListenerConnectionState { DISCONNECTED, CONNECTED, ACCESS_REVOKED }

enum class CaptureResult { REJECTED, PROCESSOR_UNAVAILABLE, ACCEPTED, FAILED }

/** Synchronous local boundary. Implementations must not retain, log, persist, or upload raw input. */
fun interface NotificationCaptureSink {
    fun accept(notification: ObservedNotification): CaptureResult
}

/** Caller serializes callbacks and settings updates; only connection metadata is retained here. */
class NotificationCaptureCoordinator(
    private val settings: () -> NotificationCaptureSettings,
    private val sink: NotificationCaptureSink,
    private val isProcessorAvailable: () -> Boolean = { true },
) {
    var connectionState: ListenerConnectionState = ListenerConnectionState.DISCONNECTED
        private set
    var lastCaptureResult: CaptureResult? = null
        private set

    fun updateConnection(isConnected: Boolean, hasAccess: Boolean) {
        connectionState = when {
            !hasAccess -> ListenerConnectionState.ACCESS_REVOKED
            isConnected -> ListenerConnectionState.CONNECTED
            else -> ListenerConnectionState.DISCONNECTED
        }
    }

    /** Reads content only after source admission; the lazy input must not escape this call. */
    fun capture(
        source: NotificationSource,
        postedAtEpochMilliseconds: Long,
        readContent: () -> NotificationContent,
    ): CaptureResult {
        return record(try {
            when {
                connectionState != ListenerConnectionState.CONNECTED || !settings().admits(source) ->
                    CaptureResult.REJECTED
                !isProcessorAvailable() -> CaptureResult.PROCESSOR_UNAVAILABLE
                else -> sink.accept(
                    ObservedNotification(source, postedAtEpochMilliseconds, readContent()),
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            CaptureResult.FAILED
        })
    }

    private fun record(result: CaptureResult): CaptureResult {
        lastCaptureResult = result
        return result
    }
}
