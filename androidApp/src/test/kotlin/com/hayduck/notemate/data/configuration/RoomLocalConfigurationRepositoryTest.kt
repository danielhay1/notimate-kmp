package com.hayduck.notemate.data.configuration

import android.app.Application
import android.content.Context
import com.hayduck.notemate.domain.automation.Automation
import com.hayduck.notemate.domain.automation.AutomationAction
import com.hayduck.notemate.domain.automation.AutomationProfile
import com.hayduck.notemate.domain.automation.ConfirmationPolicy
import com.hayduck.notemate.domain.automation.NotificationSourceSelector
import com.hayduck.notemate.domain.configuration.LocalConfiguration
import com.hayduck.notemate.domain.notification.NotificationClassification
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RoomLocalConfigurationRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: LocalConfigurationDatabase
    private lateinit var repository: RoomLocalConfigurationRepository

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        database = LocalConfigurationDatabase.create(context)
        repository = RoomLocalConfigurationRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun firstObservationInitializesPersonalWithReviewAndNoMonitoredSources() = runBlocking {
        val configuration = withTimeout(TIMEOUT_MILLIS) { repository.configuration.first() }
        assertEquals("personal", configuration.profiles.selectedProfileId)
        assertEquals(1, configuration.profiles.profiles.size)
        assertEquals("Personal", configuration.profiles.selectedProfile.name)
        assertFalse(configuration.isPaused)
        assertTrue(configuration.monitoredApplicationIds.isEmpty())
        val automation = configuration.profiles.selectedProfile.automations.single()
        assertTrue(automation.isEnabled)
        assertEquals(AutomationAction.PROPOSE_CALENDAR_EVENT, automation.action)
        assertEquals(ConfirmationPolicy.ALWAYS_REVIEW, automation.confirmationPolicy)
        assertEquals(NotificationSourceSelector.AnyMonitoredApplication, automation.sourceSelector)
        assertEquals(
            setOf(NotificationClassification.POSSIBLE_CALENDAR_EVENT),
            automation.conditions,
        )
        assertTrue(automation.exclusions.isEmpty())
    }

    @Test
    fun fileBackedRestartPreservesConfigurationAndOrdering() = runBlocking {
        val work = profile("z-work", "Work", listOf(
            automation("z-review", AutomationAction.GROUP_FOR_REVIEW),
            automation("a-ignore", AutomationAction.IGNORE, enabled = false),
        ))
        repository.saveProfile(work)
        repository.saveProfile(profile("a-home", "Home"))
        repository.saveProfile(profile(work.id, "Updated Work", work.automations))
        repository.saveAutomation(work.id, automation("m-calendar"))
        repository.selectProfile(work.id)
        repository.setMonitoredApplications(setOf("synthetic.calendar", "synthetic.mail"))
        repository.setPaused(true)
        val expected = repository.getConfiguration()

        reopenDatabase()

        val actual = repository.getConfiguration()
        assertConfigurationEquals(expected, actual)
        assertEquals(listOf("personal", "z-work", "a-home"), actual.profiles.profiles.map { it.id })
        assertEquals(
            listOf("z-review", "a-ignore", "m-calendar"),
            actual.profiles.selectedProfile.automations.map { it.id },
        )
        assertTrue(actual.isPaused)
        assertEquals("z-work", actual.profiles.selectedProfileId)
    }

    @Test
    fun concurrentIndependentMutationsDoNotLoseEachOthersChanges() = runBlocking {
        val work = profile("work", "Work")
        repository.saveProfile(work)
        val secondRepository = RoomLocalConfigurationRepository(database)
        val start = CompletableDeferred<Unit>()
        coroutineScope {
            launch(Dispatchers.Default) {
                start.await()
                repository.setPaused(true)
            }
            launch(Dispatchers.Default) {
                start.await()
                secondRepository.setMonitoredApplications(setOf("synthetic.mail"))
            }
            launch(Dispatchers.Default) {
                start.await()
                repository.selectProfile(work.id)
            }
            start.complete(Unit)
        }
        val actual = repository.getConfiguration()
        assertTrue(actual.isPaused)
        assertEquals(setOf("synthetic.mail"), actual.monitoredApplicationIds)
        assertEquals(work.id, actual.profiles.selectedProfileId)
    }

    @Test
    fun invalidDeletionAndOwnershipChangesPreserveExistingConfiguration() = runBlocking {
        repository.saveProfile(profile("work", "Work"))
        repository.setMonitoredApplications(setOf("synthetic.mail"))
        val expected = repository.getConfiguration()
        val personal = expected.profiles.selectedProfile
        assertFailsWith<IllegalArgumentException> { repository.deleteProfile(personal.id) }
        assertFailsWith<IllegalArgumentException> {
            repository.deleteProfile(personal.id, personal.id)
        }
        assertFailsWith<IllegalArgumentException> {
            repository.deleteProfile(personal.id, "missing")
        }
        assertFailsWith<IllegalArgumentException> {
            repository.saveAutomation("work", personal.automations.single())
        }
        assertFailsWith<IllegalArgumentException> {
            repository.deleteAutomation("work", personal.automations.single().id)
        }
        assertFailsWith<IllegalArgumentException> {
            repository.deleteAutomation(personal.id, personal.automations.single().id)
        }
        assertConfigurationEquals(expected, repository.getConfiguration())
        reopenDatabase()
        assertConfigurationEquals(expected, repository.getConfiguration())
    }

    @Test
    fun deletingSelectedProfileEmitsOnlyCompleteCommittedSnapshots() = runBlocking {
        repository.saveProfile(profile("work", "Work"))
        repository.setPaused(true)
        val snapshots = Channel<LocalConfiguration>(Channel.UNLIMITED)
        val collector = launch(Dispatchers.Default) {
            repository.configuration.collect { snapshots.send(it) }
        }
        try {
            val before = snapshots.awaitSnapshot { it.profiles.selectedProfileId == "personal" }
            assertEquals(2, before.profiles.profiles.size)
            repository.deleteProfile("personal", "work")
            val after = snapshots.awaitSnapshot { it.profiles.selectedProfileId == "work" }
            assertEquals(listOf("work"), after.profiles.profiles.map { it.id })
            assertEquals(listOf("work-automation"), after.profiles.selectedProfile.automations.map {
                it.id
            })
            assertTrue(after.isPaused)
            assertFailsWith<IllegalArgumentException> { repository.deleteProfile("work") }
            assertConfigurationEquals(after, repository.getConfiguration())
        } finally {
            collector.cancelAndJoin()
            snapshots.close()
        }
    }

    @Test
    fun corruptPersistedClassificationFailsWithoutResettingData() = runBlocking {
        repository.setPaused(true)
        repository.setMonitoredApplications(setOf("synthetic.mail"))
        withContext(Dispatchers.IO) {
            database.openHelper.writableDatabase.execSQL(
                "UPDATE automations SET conditions = ? WHERE id = ?",
                arrayOf("UNSUPPORTED_SYNTHETIC_CLASSIFICATION", "personal-calendar"),
            )
        }
        reopenDatabase()
        assertFailsWith<IllegalArgumentException> { repository.getConfiguration() }
        assertFailsWith<IllegalArgumentException> { repository.setPaused(false) }
        assertFailsWith<IllegalArgumentException> {
            withTimeout(TIMEOUT_MILLIS) { repository.configuration.first() }
        }
        withContext(Dispatchers.IO) {
            database.openHelper.readableDatabase.query(
                "SELECT isPaused FROM configuration WHERE id = 0",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            database.openHelper.readableDatabase.query(
                "SELECT conditions FROM automations WHERE id = 'personal-calendar'",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("UNSUPPORTED_SYNTHETIC_CLASSIFICATION", cursor.getString(0))
            }
            database.openHelper.readableDatabase.query(
                "SELECT applicationId FROM monitored_applications",
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("synthetic.mail", cursor.getString(0))
            }
        }
    }

    @Test
    fun configurationDatabaseIsStoredInsideNoBackupDirectory() = runBlocking {
        repository.getConfiguration()
        val path = withContext(Dispatchers.IO) {
            File(checkNotNull(database.openHelper.readableDatabase.path)).canonicalFile
        }
        assertEquals(context.noBackupFilesDir.canonicalFile, path.parentFile)
        assertTrue(path.isFile)
    }

    private fun reopenDatabase() {
        database.close()
        database = LocalConfigurationDatabase.create(context)
        repository = RoomLocalConfigurationRepository(database)
    }

    private fun profile(
        id: String,
        name: String,
        automations: List<Automation> = listOf(automation("$id-automation")),
    ): AutomationProfile = AutomationProfile(
        id = id,
        name = name,
        description = "Synthetic configuration",
        automations = automations,
    )

    private fun automation(
        id: String,
        action: AutomationAction = AutomationAction.PROPOSE_CALENDAR_EVENT,
        enabled: Boolean = true,
    ): Automation = Automation(
        id = id,
        name = "Synthetic Automation $id",
        isEnabled = enabled,
        sourceSelector = NotificationSourceSelector.Application("synthetic.calendar"),
        action = action,
        confirmationPolicy = ConfirmationPolicy.ALWAYS_REVIEW,
        conditions = setOf(
            NotificationClassification.POSSIBLE_CALENDAR_EVENT,
            NotificationClassification.REVIEW_NEEDED,
        ),
        exclusions = setOf(NotificationClassification.IGNORED, NotificationClassification.UNKNOWN),
    )

    private fun assertConfigurationEquals(expected: LocalConfiguration, actual: LocalConfiguration) {
        assertEquals(expected.isPaused, actual.isPaused)
        assertEquals(expected.monitoredApplicationIds, actual.monitoredApplicationIds)
        assertEquals(expected.profiles.selectedProfileId, actual.profiles.selectedProfileId)
        assertEquals(expected.profiles.profiles.map { it.id }, actual.profiles.profiles.map { it.id })
        expected.profiles.profiles.zip(actual.profiles.profiles).forEach { (expectedProfile, profile) ->
            assertEquals(expectedProfile.name, profile.name)
            assertEquals(expectedProfile.description, profile.description)
            assertEquals(expectedProfile.automations.map { it.id }, profile.automations.map { it.id })
            expectedProfile.automations.zip(profile.automations).forEach { (expectedAutomation, item) ->
                assertEquals(expectedAutomation.name, item.name)
                assertEquals(expectedAutomation.isEnabled, item.isEnabled)
                assertEquals(expectedAutomation.sourceSelector, item.sourceSelector)
                assertEquals(expectedAutomation.action, item.action)
                assertEquals(expectedAutomation.confirmationPolicy, item.confirmationPolicy)
                assertEquals(expectedAutomation.conditions, item.conditions)
                assertEquals(expectedAutomation.exclusions, item.exclusions)
            }
        }
    }

    private suspend fun Channel<LocalConfiguration>.awaitSnapshot(
        predicate: (LocalConfiguration) -> Boolean,
    ): LocalConfiguration = withTimeout(TIMEOUT_MILLIS) {
        var snapshot = receive()
        while (!predicate(snapshot)) snapshot = receive()
        snapshot
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
