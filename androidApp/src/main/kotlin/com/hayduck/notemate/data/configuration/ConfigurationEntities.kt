package com.hayduck.notemate.data.configuration

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.hayduck.notemate.domain.automation.AutomationAction
import com.hayduck.notemate.domain.automation.ConfirmationPolicy
import com.hayduck.notemate.domain.notification.NotificationClassification

@Entity(tableName = "configuration")
internal data class ConfigurationEntity(
    @PrimaryKey val id: Int = 0,
    val selectedProfileId: String,
    val isPaused: Boolean,
)

@Entity(
    tableName = "profiles",
    foreignKeys = [ForeignKey(
        entity = ConfigurationEntity::class,
        parentColumns = ["id"],
        childColumns = ["configurationId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("configurationId")],
)
internal data class ProfileEntity(
    @PrimaryKey val id: String,
    val configurationId: Int = 0,
    val name: String,
    val description: String?,
    val position: Int,
)

@Entity(
    tableName = "automations",
    foreignKeys = [ForeignKey(
        entity = ProfileEntity::class,
        parentColumns = ["id"],
        childColumns = ["profileId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("profileId")],
)
internal data class AutomationEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val name: String,
    val isEnabled: Boolean,
    val usesAnyMonitoredApplication: Boolean,
    val applicationId: String?,
    val action: AutomationAction,
    val confirmationPolicy: ConfirmationPolicy,
    val conditions: Set<NotificationClassification>,
    val exclusions: Set<NotificationClassification>,
    val position: Int,
)

@Entity(
    tableName = "monitored_applications",
    foreignKeys = [ForeignKey(
        entity = ConfigurationEntity::class,
        parentColumns = ["id"],
        childColumns = ["configurationId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("configurationId")],
)
internal data class MonitoredApplicationEntity(
    @PrimaryKey val applicationId: String,
    val configurationId: Int = 0,
)

internal data class ProfileWithAutomations(
    @Embedded val profile: ProfileEntity,
    @Relation(parentColumn = "id", entityColumn = "profileId")
    val automations: List<AutomationEntity>,
)

internal data class ConfigurationSnapshot(
    @Embedded val settings: ConfigurationEntity,
    @Relation(
        entity = ProfileEntity::class,
        parentColumn = "id",
        entityColumn = "configurationId",
    )
    val profiles: List<ProfileWithAutomations>,
    @Relation(parentColumn = "id", entityColumn = "configurationId")
    val monitoredApplications: List<MonitoredApplicationEntity>,
)
