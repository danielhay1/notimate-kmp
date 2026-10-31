package com.hayduck.notemate.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.hayduck.notemate.NotiMateApplication
import com.hayduck.notemate.domain.notification.NotificationCaptureCoordinator
import com.hayduck.notemate.domain.notification.NotificationSource
import com.hayduck.notemate.permissions.hasNotificationAccess

class NotiMateNotificationListenerService : NotificationListenerService() {
    private lateinit var coordinator: NotificationCaptureCoordinator
    private val mapper = AndroidNotificationContentMapper()

    override fun onCreate() {
        super.onCreate()
        coordinator = (application as NotiMateApplication).captureCoordinator
    }

    override fun onListenerConnected() {
        coordinator.updateConnection(isConnected = true, hasAccess = hasNotificationAccess(this))
    }

    override fun onListenerDisconnected() {
        coordinator.updateConnection(isConnected = false, hasAccess = hasNotificationAccess(this))
    }

    override fun onDestroy() {
        coordinator.updateConnection(isConnected = false, hasAccess = hasNotificationAccess(this))
        super.onDestroy()
    }

    override fun onNotificationPosted(notification: StatusBarNotification?) {
        if (notification == null) return
        if (!hasNotificationAccess(this)) {
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
}
