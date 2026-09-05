package com.hayduck.notemate.domain.automation

import com.hayduck.notemate.domain.notification.NotificationSource

data class Automation(
    val id: String,
    val name: String,
    val isEnabled: Boolean,
    val sourceSelector: NotificationSourceSelector,
    val action: AutomationAction,
    val confirmationPolicy: ConfirmationPolicy = ConfirmationPolicy.ALWAYS_REVIEW,
) {
    init {
        require(id.isNotBlank()) { "Automation identifier must not be blank." }
        require(name.isNotBlank()) { "Automation name must not be blank." }
    }
}

sealed interface NotificationSourceSelector {
    fun matches(source: NotificationSource): Boolean

    data object AnyMonitoredApplication : NotificationSourceSelector {
        override fun matches(source: NotificationSource): Boolean = true
    }

    data class Application(
        val applicationId: String,
    ) : NotificationSourceSelector {
        init {
            require(applicationId.isNotBlank()) { "Application identifier must not be blank." }
        }

        override fun matches(source: NotificationSource): Boolean =
            source.applicationId == applicationId
    }
}

enum class AutomationAction {
    PROPOSE_CALENDAR_EVENT,
    IGNORE,
    GROUP_FOR_REVIEW,
}

enum class ConfirmationPolicy {
    ALWAYS_REVIEW,
}
