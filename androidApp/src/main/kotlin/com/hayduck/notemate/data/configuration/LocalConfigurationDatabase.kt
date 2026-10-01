package com.hayduck.notemate.data.configuration

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import java.io.File

@Database(
    entities = [ConfigurationEntity::class, ProfileEntity::class, AutomationEntity::class,
        MonitoredApplicationEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(ConfigurationConverters::class)
internal abstract class LocalConfigurationDatabase : RoomDatabase() {
    abstract fun configurationDao(): LocalConfigurationDao

    companion object {
        /** Configuration is local-only; Android automatic backup must not copy this database. */
        fun create(context: Context): LocalConfigurationDatabase = Room.databaseBuilder(
            context.applicationContext,
            LocalConfigurationDatabase::class.java,
            File(context.noBackupFilesDir, "local-configuration.db").absolutePath,
        ).build()
    }
}
