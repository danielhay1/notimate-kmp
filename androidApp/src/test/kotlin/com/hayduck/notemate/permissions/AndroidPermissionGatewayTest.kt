package com.hayduck.notemate.permissions

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.hayduck.notemate.notification.NotiMateNotificationListenerService
import com.hayduck.notemate.today.ProposalAlertPermission
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class AndroidPermissionGatewayTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    @Test
    fun notificationAccessIsIndependentOfAlertPermission() {
        val gateway = AndroidPermissionGateway(context)
        val notifications = context.getSystemService(NotificationManager::class.java)
        assertFalse(gateway.snapshot(false).hasNotificationAccess)
        shadowOf(notifications).setNotificationListenerAccessGranted(
            ComponentName(context, NotiMateNotificationListenerService::class.java), true,
        )
        shadowOf(notifications).setNotificationsEnabled(false)
        val snapshot = gateway.snapshot(true)
        assertTrue(snapshot.hasNotificationAccess)
        assertEquals(ProposalAlertPermission.APP_DISABLED, snapshot.alerts)
    }

    @Test
    @Config(sdk = [24])
    fun legacyAccessQueryRequiresThisListenerAndSettingsUseAppDetails() {
        val gateway = AndroidPermissionGateway(context)
        Settings.Secure.putString(
            context.contentResolver, "enabled_notification_listeners", "synthetic.other/.Listener",
        )
        assertFalse(gateway.snapshot(false).hasNotificationAccess)
        val component = ComponentName(context, NotiMateNotificationListenerService::class.java)
        Settings.Secure.putString(
            context.contentResolver, "enabled_notification_listeners", component.flattenToString(),
        )
        assertTrue(gateway.snapshot(false).hasNotificationAccess)
        val intent = gateway.proposalAlertsIntent()
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:${context.packageName}", intent.data.toString())
    }

    @Test
    @Config(sdk = [33])
    fun runtimeDenialAndGrantedAlertsFollowCurrentOsState() {
        val gateway = AndroidPermissionGateway(context)
        assertEquals(ProposalAlertPermission.NOT_REQUESTED, gateway.snapshot(false).alerts)
        assertEquals(ProposalAlertPermission.DENIED, gateway.snapshot(true).alerts)
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(
            Manifest.permission.POST_NOTIFICATIONS,
        )
        assertEquals(ProposalAlertPermission.GRANTED, gateway.snapshot(false).alerts)
        shadowOf(RuntimeEnvironment.getApplication()).denyPermissions(
            Manifest.permission.POST_NOTIFICATIONS,
        )
        assertEquals(ProposalAlertPermission.DENIED, gateway.snapshot(true).alerts)
    }

    @Test
    fun missingChannelIsNotBlockedAndExistingDisabledChannelIsDistinct() {
        val gateway = AndroidPermissionGateway(context)
        val notifications = context.getSystemService(NotificationManager::class.java)
        assertEquals(ProposalAlertPermission.GRANTED, gateway.snapshot(false).alerts)
        assertTrue(notifications.notificationChannels.isEmpty())
        notifications.createNotificationChannel(NotificationChannel(
            AndroidPermissionGateway.PROPOSAL_CHANNEL_ID,
            "Synthetic proposal channel",
            NotificationManager.IMPORTANCE_NONE,
        ))
        assertEquals(ProposalAlertPermission.CHANNEL_DISABLED, gateway.snapshot(false).alerts)
        assertEquals(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
            gateway.notificationAccessIntent().action)
        val intent = gateway.proposalAlertsIntent()
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.action)
        assertEquals(context.packageName, intent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
    }
}
