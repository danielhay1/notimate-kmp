package com.hayduck.notemate.notification

import android.app.NotificationManager
import android.content.ComponentName
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.hayduck.notemate.NotiMateApplication
import com.hayduck.notemate.domain.notification.NotificationCaptureCoordinator
import com.hayduck.notemate.domain.notification.NotificationSource

class NotiMateNotificationListenerService : NotificationListenerService() {
    private lateinit var coordinator: NotificationCaptureCoordinator
    private val mapper = AndroidNotificationContentMapper()

    override fun onCreate() {
        super.onCreate()
        coordinator = (application as NotiMateApplication).captureCoordinator
    }

    override fun onListenerConnected() {
        coordinator.updateConnection(isConnected = true, hasAccess = hasNotificationAccess())
    }

    override fun onListenerDisconnected() {
        coordinator.updateConnection(isConnected = false, hasAccess = hasNotificationAccess())
    }

    override fun onDestroy() {
        coordinator.updateConnection(isConnected = false, hasAccess = hasNotificationAccess())
        super.onDestroy()
    }

    override fun onNotificationPosted(notification: StatusBarNotification?) {
        if (notification == null) return
        if (!hasNotificationAccess()) {
            coordinator.updateConnection(isConnected = false, hasAccess = false)
            return
        }
        val applicationId = notification.packageName?.takeIf { it.isNotBlank() } ?: return
        if (applicationId == packageName) return
        coordinator.capture(NotificationSource(applicationId), notification.postTime) {
            val extras = notification.notification.extras
            mapper.map(extras::getCharSequence)
        }
    }

    private fun hasNotificationAccess(): Boolean {
        val component = ComponentName(this, NotiMateNotificationListenerService::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            getSystemService(NotificationManager::class.java)
                .isNotificationListenerAccessGranted(component)
        } else {
            Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
                ?.split(':')?.any { ComponentName.unflattenFromString(it) == component } == true
        }
    }
}
