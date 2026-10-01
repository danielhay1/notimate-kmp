package com.hayduck.notemate.domain.configuration

import com.hayduck.notemate.domain.automation.Automation
import com.hayduck.notemate.domain.automation.AutomationProfile
import kotlinx.coroutines.flow.Flow

/**
 * Durable local source of truth for complete configuration snapshots.
 *
 * All operations are main-safe. Reads and observation initialize a missing installation with
 * [LocalConfiguration.initial]. Each mutation validates and commits atomically against current
 * stored configuration. Invalid mutations and storage failures propagate to the caller; existing
 * data is never silently reset. Cancellation follows the storage transaction's commit boundary.
 */
interface LocalConfigurationRepository {
    /** Emits committed snapshots only; initialization and storage failures reach collectors. */
    val configuration: Flow<LocalConfiguration>

    suspend fun getConfiguration(): LocalConfiguration

    suspend fun setPaused(isPaused: Boolean)

    suspend fun setMonitoredApplications(applicationIds: Set<String>)

    suspend fun selectProfile(profileId: String)

    /** Replaces or appends a Profile, preserving selection and rejecting shared ownership. */
    suspend fun saveProfile(profile: AutomationProfile)

    /** Atomically removes the Profile and selects the required replacement when it was selected. */
    suspend fun deleteProfile(profileId: String, replacementProfileId: String? = null)

    /** Replaces in place or appends within the existing owner; does not transfer ownership. */
    suspend fun saveAutomation(profileId: String, automation: Automation)

    /** Rejects missing identifiers, wrong ownership, and deletion of the last Automation. */
    suspend fun deleteAutomation(profileId: String, automationId: String)
}
