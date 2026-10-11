package com.hayduck.notemate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayduck.notemate.featurekeys.FeatureKeyStartup
import com.hayduck.notemate.featurekeys.notificationTestingAction

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val manager = (application as NotiMateApplication).featureKeys
        setContent {
            FeatureKeyStartup(manager, showEditor = false) {
                val keys by manager.keys.collectAsStateWithLifecycle()
                App(onOpenNotificationTesting = keys.takeIf { it.isNotEmpty() }?.let {
                    notificationTestingAction(this@MainActivity, manager)
                })
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
