package com.hayduck.notemate

import android.app.Application
import com.hayduck.notemate.data.configuration.LocalConfigurationDatabase
import com.hayduck.notemate.data.configuration.RoomLocalConfigurationRepository
import com.hayduck.notemate.data.results.LocalResultsDatabase
import com.hayduck.notemate.data.results.RoomLocalResultsRepository
import com.hayduck.notemate.domain.configuration.LocalConfigurationRepository
import com.hayduck.notemate.domain.notification.CaptureResult
import com.hayduck.notemate.domain.notification.NotificationCaptureCoordinator
import com.hayduck.notemate.domain.notification.NotificationCaptureSink
import com.hayduck.notemate.domain.results.LocalResultsRepository
import com.hayduck.notemate.featurekeys.FeatureKeyManager
import com.hayduck.notemate.featurekeys.createFeatureKeyManager
import com.hayduck.notemate.notification.CaptureSettingsObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class NotiMateApplication : Application() {
    internal lateinit var featureKeys: FeatureKeyManager
        private set
    internal lateinit var configurationRepository: LocalConfigurationRepository
        private set
    internal lateinit var resultsRepository: LocalResultsRepository
        private set
    internal lateinit var captureCoordinator: NotificationCaptureCoordinator
        private set

    override fun onCreate() {
        super.onCreate()
        featureKeys = createFeatureKeyManager(this)
        configurationRepository = RoomLocalConfigurationRepository(
            LocalConfigurationDatabase.create(this),
        )
        resultsRepository = RoomLocalResultsRepository(LocalResultsDatabase.create(this))
        val settingsObserver = CaptureSettingsObserver(
            configurationRepository.configuration,
            CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        )
        captureCoordinator = NotificationCaptureCoordinator(
            settings = { settingsObserver.settings.value },
            sink = NotificationCaptureSink { CaptureResult.PROCESSOR_UNAVAILABLE },
            isProcessorAvailable = { false },
        )
    }
}
