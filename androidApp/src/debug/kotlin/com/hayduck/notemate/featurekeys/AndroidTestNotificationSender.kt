package com.hayduck.notemate.featurekeys

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.hayduck.notemate.MainActivity
import com.hayduck.notemate.R

internal class AndroidTestNotificationSender(
    private val context: Context,
    private val featureKeys: FeatureKeyManager,
) {
    private val notifications = context.getSystemService(NotificationManager::class.java)

    fun send(input: NotificationTestUiState): NotificationTestResult {
        if (!featureKeys.isDebug ||
            !featureKeys.isEnabled(FeatureKeys.NOTIFICATION_TESTING_ENABLED)
        ) return NotificationTestResult.DISABLED
        if (!input.isValid) return NotificationTestResult.INVALID
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) return NotificationTestResult.PERMISSION_REQUIRED

        return try {
            if (!notifications.areNotificationsEnabled()) return NotificationTestResult.BLOCKED
            if (Build.VERSION.SDK_INT >= 26) {
                notifications.createNotificationChannel(NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_test_channel),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ))
                if (notifications.getNotificationChannel(CHANNEL_ID)?.importance ==
                    NotificationManager.IMPORTANCE_NONE
                ) return NotificationTestResult.BLOCKED
            }
            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
                ),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val publicVersion = builder()
                .setSmallIcon(R.drawable.ic_notification_test)
                .setContentTitle(context.getString(R.string.notification_test_hidden))
                .build()
            val notification = builder()
                .setSmallIcon(R.drawable.ic_notification_test)
                .setContentTitle(input.title)
                .setContentText(input.body)
                .setStyle(Notification.BigTextStyle().bigText(input.body))
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setPublicVersion(publicVersion)
                .setAutoCancel(true)
                .setContentIntent(openApp)
                .build()
            notifications.notify(NOTIFICATION_ID, notification)
            NotificationTestResult.POSTED
        } catch (_: SecurityException) {
            NotificationTestResult.BLOCKED
        } catch (_: RuntimeException) {
            NotificationTestResult.FAILED
        }
    }

    @Suppress("DEPRECATION")
    private fun builder(): Notification.Builder = if (Build.VERSION.SDK_INT >= 26) {
        Notification.Builder(context, CHANNEL_ID)
    } else {
        Notification.Builder(context)
    }

    companion object {
        const val CHANNEL_ID = "manual_notification_tests"
        const val NOTIFICATION_ID = 9001
    }
}
