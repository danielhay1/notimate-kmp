package com.hayduck.notemate.notification

import com.hayduck.notemate.domain.notification.NotificationContent
import com.hayduck.notemate.domain.notification.NotificationContentMinimizer

/** Reads only current visible fields after admission, never message history or pending intents. */
internal class AndroidNotificationContentMapper {
    private val minimizer = NotificationContentMinimizer(256, 4096)

    fun map(readText: (String) -> CharSequence?): NotificationContent = minimizer.minimize(
        title = readText("android.title"),
        body = readText("android.bigText") ?: readText("android.text"),
    )
}
