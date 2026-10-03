package com.hayduck.notemate.data.results

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

@Database(
    entities = [ResultsScopeEntity::class, StoredProposalEntity::class,
        StoredActivityEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class LocalResultsDatabase : RoomDatabase() {
    abstract fun resultsDao(): LocalResultsDao

    companion object {
        /** Structured local results are excluded from Android automatic backup. */
        fun create(context: Context): LocalResultsDatabase = Room.databaseBuilder(
            context.applicationContext,
            LocalResultsDatabase::class.java,
            File(context.noBackupFilesDir, "local-results.db").absolutePath,
        ).build()
    }
}
