package com.hayduck.notemate

import android.app.Application
import com.hayduck.notemate.domain.notification.CaptureResult
import com.hayduck.notemate.domain.notification.NotificationCaptureCoordinator
import com.hayduck.notemate.domain.notification.NotificationCaptureSettings
import com.hayduck.notemate.domain.notification.NotificationCaptureSink

class NotiMateApplication : Application() {
    internal lateinit var captureCoordinator: NotificationCaptureCoordinator
        private set

    override fun onCreate() {
        super.onCreate()
        val unloadedSettings = NotificationCaptureSettings(emptySet(), null, isPaused = false)
        captureCoordinator = NotificationCaptureCoordinator(
            settings = { unloadedSettings },
            sink = NotificationCaptureSink { CaptureResult.PROCESSOR_UNAVAILABLE },
            isProcessorAvailable = { false },
        )
    }
}
