package com.hayduck.notemate.domain.notification

/**
 * Minimized notification data produced by the Android boundary only after the
 * source application has passed the global monitored-source check.
 *
 * This model is transient input for on-device processing. It must not be
 * persisted or logged.
 */
data class ObservedNotification(
    val source: NotificationSource,
    val postedAtEpochMilliseconds: Long,
    val content: NotificationContent,
) {
    override fun toString(): String =
        "ObservedNotification(source=$source, " +
            "postedAtEpochMilliseconds=$postedAtEpochMilliseconds, content=[REDACTED])"
}

data class NotificationSource(
    val applicationId: String,
    val notificationCategory: String? = null,
) {
    init {
        require(applicationId.isNotBlank()) { "Application identifier must not be blank." }
    }
}

data class NotificationContent(
    val title: String?,
    val body: String?,
) {
    override fun toString(): String = "NotificationContent([REDACTED])"
}
