package com.hayduck.notemate.featurekeys

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class AndroidTestNotificationSenderTest {
    private val input = NotificationTestUiState("Synthetic title", "Synthetic body")

    @Test
    fun disabledKeyPreventsPostingAndEnabledKeyUsesPrivatePreview() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val manager = manager(context)
        val sender = AndroidTestNotificationSender(context, manager)
        val notifications = context.getSystemService(NotificationManager::class.java)
        assertEquals(NotificationTestResult.DISABLED, sender.send(input))
        assertNull(shadowOf(notifications).getNotification(AndroidTestNotificationSender.NOTIFICATION_ID))
        manager.applyEdits(mapOf(FeatureKeys.NOTIFICATION_TESTING_ENABLED to "true"))
        assertEquals(NotificationTestResult.POSTED, sender.send(input))
        val posted = assertNotNull(shadowOf(notifications)
            .getNotification(AndroidTestNotificationSender.NOTIFICATION_ID))
        assertEquals(Notification.VISIBILITY_PRIVATE, posted.visibility)
        assertEquals("Synthetic title", posted.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Synthetic body", posted.extras.getString(Notification.EXTRA_TEXT))
        assertEquals("Test notification", posted.publicVersion.extras
            .getString(Notification.EXTRA_TITLE))
        assertNull(posted.publicVersion.extras.getString(Notification.EXTRA_TEXT))
        assertNotNull(posted.contentIntent)
    }

    @Test
    fun blockedChannelAndInvalidFormDoNotReportSuccess() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val manager = manager(context)
        manager.applyEdits(mapOf(FeatureKeys.NOTIFICATION_TESTING_ENABLED to "true"))
        val sender = AndroidTestNotificationSender(context, manager)
        assertEquals(NotificationTestResult.INVALID, sender.send(NotificationTestUiState()))
        val notifications = context.getSystemService(NotificationManager::class.java)
        notifications.createNotificationChannel(NotificationChannel(
            AndroidTestNotificationSender.CHANNEL_ID, "Synthetic channel",
            NotificationManager.IMPORTANCE_NONE,
        ))
        assertEquals(NotificationTestResult.BLOCKED, sender.send(input))
        assertNull(shadowOf(notifications).getNotification(AndroidTestNotificationSender.NOTIFICATION_ID))
    }

    @Test
    @Config(sdk = [33])
    fun missingRuntimePermissionRequiresUserActionBeforePosting() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val manager = manager(context)
        manager.applyEdits(mapOf(FeatureKeys.NOTIFICATION_TESTING_ENABLED to "true"))
        val notifications = context.getSystemService(NotificationManager::class.java)
        val sender = AndroidTestNotificationSender(context, manager)
        assertEquals(NotificationTestResult.PERMISSION_REQUIRED, sender.send(input))
        assertNull(shadowOf(notifications).getNotification(AndroidTestNotificationSender.NOTIFICATION_ID))
    }

    private suspend fun manager(context: Context): FeatureKeyManager =
        FeatureKeyManager(true, AndroidFeatureKeyOverrideStore(context)).also {
            it.initialize("""[
                {"key":"notificationTestingEnabled","type":"boolean","default":false}
            ]""")
        }
}
