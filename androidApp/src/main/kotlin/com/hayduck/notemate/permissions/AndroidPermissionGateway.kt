package com.hayduck.notemate.permissions

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.hayduck.notemate.notification.NotiMateNotificationListenerService
import com.hayduck.notemate.today.PermissionSnapshot
import com.hayduck.notemate.today.ProposalAlertPermission

/** Reads current Android access; request history never substitutes for an OS permission check. */
internal class AndroidPermissionGateway(context: Context) {
    private val context = context.applicationContext
    private val notifications = this.context.getSystemService(NotificationManager::class.java)

    fun snapshot(hasRequestedAlerts: Boolean): PermissionSnapshot = PermissionSnapshot(
        hasNotificationAccess = hasNotificationAccess(context),
        alerts = when {
            Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED -> if (hasRequestedAlerts) {
                ProposalAlertPermission.DENIED
            } else {
                ProposalAlertPermission.NOT_REQUESTED
            }
            !notifications.areNotificationsEnabled() -> ProposalAlertPermission.APP_DISABLED
            Build.VERSION.SDK_INT >= 26 &&
                notifications.getNotificationChannel(PROPOSAL_CHANNEL_ID)?.importance ==
                NotificationManager.IMPORTANCE_NONE -> ProposalAlertPermission.CHANNEL_DISABLED
            else -> ProposalAlertPermission.GRANTED
        },
    )

    fun notificationAccessIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    fun proposalAlertsIntent(): Intent = if (Build.VERSION.SDK_INT >= 26) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        )
    }

    companion object {
        const val PROPOSAL_CHANNEL_ID = "action_proposals"
    }
}

internal fun hasNotificationAccess(context: Context): Boolean {
    val component = ComponentName(context, NotiMateNotificationListenerService::class.java)
    return if (Build.VERSION.SDK_INT >= 27) {
        context.getSystemService(NotificationManager::class.java)
            .isNotificationListenerAccessGranted(component)
    } else {
        Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            ?.split(':')?.any { ComponentName.unflattenFromString(it) == component } == true
    }
}
