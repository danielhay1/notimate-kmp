package com.hayduck.notemate.data.configuration

import androidx.room.withTransaction
import com.hayduck.notemate.domain.automation.Automation
import com.hayduck.notemate.domain.automation.AutomationProfile
import com.hayduck.notemate.domain.configuration.LocalConfiguration
import com.hayduck.notemate.domain.configuration.LocalConfigurationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

internal class RoomLocalConfigurationRepository(
    private val database: LocalConfigurationDatabase,
) : LocalConfigurationRepository {
    private val dao = database.configurationDao()

    override val configuration: Flow<LocalConfiguration> = flow {
        getConfiguration()
        emitAll(dao.observeSnapshot().map { snapshot ->
            checkNotNull(snapshot) { "Local configuration is missing." }.toDomain()
        })
    }

    override suspend fun getConfiguration(): LocalConfiguration = database.withTransaction {
        readOrInitialize()
    }

    override suspend fun setPaused(isPaused: Boolean) = update { it.withPaused(isPaused) }

    override suspend fun setMonitoredApplications(applicationIds: Set<String>) {
        val ownedIds = applicationIds.toSet()
        update { it.withMonitoredApplications(ownedIds) }
    }

    override suspend fun selectProfile(profileId: String) = update { it.selectProfile(profileId) }

    override suspend fun saveProfile(profile: AutomationProfile) = update { it.saveProfile(profile) }

    override suspend fun deleteProfile(profileId: String, replacementProfileId: String?) = update {
        it.deleteProfile(profileId, replacementProfileId)
    }

    override suspend fun saveAutomation(profileId: String, automation: Automation) = update {
        it.saveAutomation(profileId, automation)
    }

    override suspend fun deleteAutomation(profileId: String, automationId: String) = update {
        it.deleteAutomation(profileId, automationId)
    }

    private suspend fun update(transform: (LocalConfiguration) -> LocalConfiguration) {
        database.withTransaction {
            persist(transform(readOrInitialize()))
        }
    }

    private suspend fun readOrInitialize(): LocalConfiguration {
        val snapshot = dao.readSnapshot()
        if (snapshot != null) return snapshot.toDomain()
        return LocalConfiguration.initial().also { persist(it) }
    }

    // Only configuration tables are replaced; proposal origins remain independent snapshots.
    private suspend fun persist(configuration: LocalConfiguration) {
        dao.upsertSettings(ConfigurationEntity(
            selectedProfileId = configuration.profiles.selectedProfileId,
            isPaused = configuration.isPaused,
        ))
        dao.deleteProfiles()
        dao.deleteMonitoredApplications()
        dao.insertProfiles(configuration.profiles.profiles.mapIndexed { position, profile ->
            ProfileEntity(
                id = profile.id,
                name = profile.name,
                description = profile.description,
                position = position,
            )
        })
        dao.insertAutomations(configuration.profiles.profiles.flatMap { profile ->
            profile.automations.mapIndexed { position, automation ->
                automation.toEntity(profile.id, position)
            }
        })
        dao.insertMonitoredApplications(configuration.monitoredApplicationIds.map {
            MonitoredApplicationEntity(it)
        })
    }
}
