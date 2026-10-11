package com.hayduck.notemate.featurekeys

import android.app.Activity
import android.content.Context

internal fun createFeatureKeyManager(context: Context): FeatureKeyManager = FeatureKeyManager(
    isDebug = false,
)

internal fun notificationTestingAction(
    activity: Activity,
    manager: FeatureKeyManager,
): (() -> Unit)? = null
