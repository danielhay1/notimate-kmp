package com.hayduck.notemate

import androidx.compose.ui.window.ComposeUIViewController
import com.hayduck.notemate.featurekeys.FeatureKeyManager
import com.hayduck.notemate.featurekeys.FeatureKeyStartup
import com.hayduck.notemate.featurekeys.IosFeatureKeyOverrideStore

fun MainViewController(isDebug: Boolean) = FeatureKeyManager(
    isDebug = isDebug,
    overrideStore = if (isDebug) IosFeatureKeyOverrideStore() else null,
).let { manager ->
    ComposeUIViewController {
        FeatureKeyStartup(manager, showEditor = isDebug) { App() }
    }
}
