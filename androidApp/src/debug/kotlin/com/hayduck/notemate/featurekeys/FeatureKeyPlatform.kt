package com.hayduck.notemate.featurekeys

import android.app.Activity
import android.content.Context
import android.content.Intent

internal fun createFeatureKeyManager(context: Context): FeatureKeyManager = FeatureKeyManager(
    isDebug = true,
    overrideStore = AndroidFeatureKeyOverrideStore(context),
)

internal fun notificationTestingAction(
    activity: Activity,
    manager: FeatureKeyManager,
): (() -> Unit)? = if (manager.isEnabled(FeatureKeys.NOTIFICATION_TESTING_ENABLED)) {
    {
        if (manager.isEnabled(FeatureKeys.NOTIFICATION_TESTING_ENABLED)) {
            activity.startActivity(Intent(activity, NotificationTestActivity::class.java))
        }
    }
} else null
