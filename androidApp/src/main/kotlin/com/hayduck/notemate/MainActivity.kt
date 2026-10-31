package com.hayduck.notemate

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hayduck.notemate.featurekeys.FeatureKeyStartup
import com.hayduck.notemate.featurekeys.notificationTestingAction
import com.hayduck.notemate.permissions.AndroidPermissionGateway
import com.hayduck.notemate.permissions.AndroidTodayTextFormatter
import com.hayduck.notemate.today.TodayAction
import com.hayduck.notemate.today.TodayApp
import com.hayduck.notemate.today.TodayDestination
import com.hayduck.notemate.today.TodayEffect
import com.hayduck.notemate.today.TodayViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val app = application as NotiMateApplication
        val gateway = AndroidPermissionGateway(app)
        val formatter = AndroidTodayTextFormatter(app)
        setContent {
            FeatureKeyStartup(app.featureKeys, showEditor = false) {
                val model = viewModel {
                    TodayViewModel(
                        app.configurationRepository, app.resultsRepository, app.onboardingStore,
                    )
                }
                val screen = model.screen
                val state by screen.state.collectAsStateWithLifecycle()
                val keys by app.featureKeys.keys.collectAsStateWithLifecycle()
                val settings = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) { model.refreshPermissions(gateway) }
                val alerts = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { model.refreshPermissions(gateway) }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                    model.refreshPermissions(gateway)
                }
                LaunchedEffect(state.onboarding) { model.refreshPermissions(gateway) }
                LaunchedEffect(model) {
                    lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        screen.effects.collect { effect ->
                            try {
                                when (effect) {
                                    TodayEffect.OPEN_ACCESS_SETTINGS ->
                                        settings.launch(gateway.notificationAccessIntent())
                                    TodayEffect.OPEN_ALERT_SETTINGS ->
                                        settings.launch(gateway.proposalAlertsIntent())
                                    TodayEffect.REQUEST_ALERT_PERMISSION -> {
                                        if (Build.VERSION.SDK_INT >= 33) {
                                            alerts.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            settings.launch(gateway.proposalAlertsIntent())
                                        }
                                    }
                                }
                            } catch (_: RuntimeException) {
                                screen.settingsUnavailable()
                            }
                        }
                    }
                }
                BackHandler(
                    enabled = state.destination !in setOf(
                        TodayDestination.WELCOME, TodayDestination.TODAY,
                    ),
                ) {
                    screen.onAction(if (state.destination == TodayDestination.PERMISSIONS) {
                        TodayAction.BACK_TO_TODAY
                    } else TodayAction.GO_BACK)
                }
                TodayApp(
                    state = state,
                    onAction = screen::onAction,
                    formatter = formatter,
                    onOpenNotificationTesting = keys.takeIf { it.isNotEmpty() }?.let {
                        notificationTestingAction(this@MainActivity, app.featureKeys)
                    },
                )
            }
        }
    }
}
