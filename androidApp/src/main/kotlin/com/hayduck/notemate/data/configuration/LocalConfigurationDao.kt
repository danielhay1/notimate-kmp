package com.hayduck.notemate.data.configuration

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface LocalConfigurationDao {
    @Transaction
    @Query("SELECT * FROM configuration WHERE id = 0")
    fun observeSnapshot(): Flow<ConfigurationSnapshot?>

    @Transaction
    @Query("SELECT * FROM configuration WHERE id = 0")
    suspend fun readSnapshot(): ConfigurationSnapshot?

    @Upsert
    suspend fun upsertSettings(settings: ConfigurationEntity)

    @Insert
    suspend fun insertProfiles(profiles: List<ProfileEntity>)

    @Insert
    suspend fun insertAutomations(automations: List<AutomationEntity>)

    @Insert
    suspend fun insertMonitoredApplications(applications: List<MonitoredApplicationEntity>)

    @Query("DELETE FROM profiles")
    suspend fun deleteProfiles()

    @Query("DELETE FROM monitored_applications")
    suspend fun deleteMonitoredApplications()
}
