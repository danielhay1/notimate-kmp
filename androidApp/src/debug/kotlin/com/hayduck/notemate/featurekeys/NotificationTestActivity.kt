package com.hayduck.notemate.featurekeys

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hayduck.notemate.NotiMateApplication

class NotificationTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val manager = (application as NotiMateApplication).featureKeys
        setContent {
            FeatureKeyStartup(manager, showEditor = false) {
                val model = viewModel {
                    NotificationTestViewModel(AndroidTestNotificationSender(application, manager))
                }
                val state by model.state.collectAsStateWithLifecycle()
                val permission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(), model::permissionResult,
                )
                NotificationTestScreen(
                    state = state,
                    onTitleChange = model::editTitle,
                    onBodyChange = model::editBody,
                    onSend = {
                        if (model.send() == NotificationTestResult.PERMISSION_REQUIRED &&
                            Build.VERSION.SDK_INT >= 33
                        ) {
                            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onSettings = {
                        val intent = if (Build.VERSION.SDK_INT >= 26) {
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(
                                Settings.EXTRA_APP_PACKAGE, packageName,
                            )
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:$packageName"))
                        }
                        startActivity(intent)
                    },
                    onBack = ::finish,
                )
            }
        }
    }
}
