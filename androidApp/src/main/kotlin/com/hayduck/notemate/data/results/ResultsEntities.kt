package com.hayduck.notemate.data.results

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "results_scope")
internal data class ResultsScopeEntity(@PrimaryKey val id: Int = 0)

@Entity(
    tableName = "proposals",
    foreignKeys = [ForeignKey(
        entity = ResultsScopeEntity::class,
        parentColumns = ["id"],
        childColumns = ["ownerId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("ownerId")],
)
internal data class StoredProposalEntity(
    @PrimaryKey val id: String,
    val ownerId: Int = 0,
    val profileId: String,
    val profileName: String,
    val automationId: String,
    val automationName: String,
    val applicationId: String,
    val applicationName: String,
    val title: String?,
    val dateYear: Int?,
    val dateMonth: Int?,
    val dateDay: Int?,
    val timeHour: Int?,
    val timeMinute: Int?,
    val location: String?,
    val ambiguousFields: String,
    val explanation: String,
    val state: String,
    val createdAtEpochMilliseconds: Long,
    val expiresAtEpochMilliseconds: Long?,
    val revision: Long,
    val updatedAtEpochMilliseconds: Long,
) {
    override fun toString(): String = "StoredProposalEntity(content=[REDACTED])"
}

@Entity(
    tableName = "activity",
    foreignKeys = [ForeignKey(
        entity = ResultsScopeEntity::class,
        parentColumns = ["id"],
        childColumns = ["ownerId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("ownerId")],
)
internal data class StoredActivityEntity(
    @PrimaryKey val id: String,
    val ownerId: Int = 0,
    val event: String,
    val occurredAtEpochMilliseconds: Long,
    val proposalId: String?,
    val profileId: String?,
    val automationId: String?,
    val applicationId: String?,
) {
    override fun toString(): String = "StoredActivityEntity(references=[REDACTED])"
}

internal data class ResultsSnapshot(
    @Embedded val scope: ResultsScopeEntity,
    @Relation(parentColumn = "id", entityColumn = "ownerId")
    val proposals: List<StoredProposalEntity>,
    @Relation(parentColumn = "id", entityColumn = "ownerId")
    val activity: List<StoredActivityEntity>,
)
