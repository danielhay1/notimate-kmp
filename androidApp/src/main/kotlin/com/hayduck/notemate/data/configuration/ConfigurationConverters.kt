package com.hayduck.notemate.data.configuration

import androidx.room.TypeConverter
import com.hayduck.notemate.domain.notification.NotificationClassification

internal class ConfigurationConverters {
    @TypeConverter
    fun classificationsToString(values: Set<NotificationClassification>): String =
        values.map { it.name }.sorted().joinToString(",")

    @TypeConverter
    fun stringToClassifications(value: String): Set<NotificationClassification> =
        if (value.isEmpty()) emptySet() else value.split(',')
            .map(NotificationClassification::valueOf).toSet()
}
