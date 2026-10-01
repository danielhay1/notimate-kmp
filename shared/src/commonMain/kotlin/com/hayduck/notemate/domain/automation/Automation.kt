package com.hayduck.notemate.domain.automation

import com.hayduck.notemate.domain.notification.NotificationClassification
import com.hayduck.notemate.domain.notification.NotificationSource

/** Local configuration; any allowed classification matches, and exclusions always win. */
class Automation(
    val id: String,
    val name: String,
    val isEnabled: Boolean,
    val sourceSelector: NotificationSourceSelector,
    val action: AutomationAction,
    val confirmationPolicy: ConfirmationPolicy = ConfirmationPolicy.ALWAYS_REVIEW,
    conditions: Set<NotificationClassification> = emptySet(),
    exclusions: Set<NotificationClassification> = emptySet(),
) {
    private val storedConditions = conditions.toSet()
    private val storedExclusions = exclusions.toSet()
    val conditions: Set<NotificationClassification> get() = storedConditions.toSet()
    val exclusions: Set<NotificationClassification> get() = storedExclusions.toSet()

    init {
        require(id.isNotBlank()) { "Automation identifier must not be blank." }
        require(name.isNotBlank()) { "Automation name must not be blank." }
    }

    fun matchesClassification(classification: NotificationClassification): Boolean =
        classification !in storedExclusions &&
            (storedConditions.isEmpty() || classification in storedConditions)
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
