package com.hayduck.notemate.featurekeys

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import com.hayduck.notemate.MainActivity
import com.hayduck.notemate.NotiMateApplication

class FeatureKeysActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val manager = (application as NotiMateApplication).featureKeys
        setContent {
            FeatureKeyStartup(manager, showEditor = true) {
                LaunchedEffect(Unit) {
                    startActivity(Intent(this@FeatureKeysActivity, MainActivity::class.java))
                    finish()
                }
            }
        }
    }
}
