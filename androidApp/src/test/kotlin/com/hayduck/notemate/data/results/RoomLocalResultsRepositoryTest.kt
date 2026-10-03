package com.hayduck.notemate.data.results

import android.app.Application
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import com.hayduck.notemate.data.configuration.LocalConfigurationDatabase
import com.hayduck.notemate.data.configuration.RoomLocalConfigurationRepository
import com.hayduck.notemate.domain.activity.ActivityEvent
import com.hayduck.notemate.domain.activity.ActivityRecord
import com.hayduck.notemate.domain.automation.Automation
import com.hayduck.notemate.domain.automation.AutomationAction
import com.hayduck.notemate.domain.automation.AutomationProfile
import com.hayduck.notemate.domain.automation.NotificationSourceSelector
import com.hayduck.notemate.domain.proposal.CalendarDate
import com.hayduck.notemate.domain.proposal.CalendarField
import com.hayduck.notemate.domain.proposal.CalendarFields
import com.hayduck.notemate.domain.proposal.CalendarProposal
import com.hayduck.notemate.domain.proposal.CalendarTime
import com.hayduck.notemate.domain.proposal.ProposalExplanation
import com.hayduck.notemate.domain.proposal.ProposalOrigin
import com.hayduck.notemate.domain.proposal.ProposalState
import com.hayduck.notemate.domain.results.LocalResults
import com.hayduck.notemate.domain.results.ProposalChange
import com.hayduck.notemate.domain.results.StoredCalendarProposal
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
class RoomLocalResultsRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: LocalResultsDatabase
    private lateinit var repository: RoomLocalResultsRepository

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        database = LocalResultsDatabase.create(context)
        repository = RoomLocalResultsRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun restartPreservesAllLifecycleStatesFieldsOriginsAndExplanations(): Unit = runBlocking {
        repository.createProposal(proposal("draft"), "create-draft")
        repository.createProposal(proposal("no-expiry", expiresAtEpochMilliseconds = null),
            "create-no-expiry")
        repository.createProposal(proposal("ready").validate(CREATED), "create-ready")
        repository.createProposal(proposal("review", incomplete = true).validate(CREATED),
            "create-review")
        val changes = listOf(
            "dismissed" to ProposalChange.Dismiss,
            "handed-off" to ProposalChange.MarkHandedOff,
            "deferred" to ProposalChange.Defer,
            "failed" to ProposalChange.Fail,
            "expired" to ProposalChange.Expire,
            "corrected" to ProposalChange.Edit(fields().copy(location = "Synthetic Room B"),
                emptySet()),
        )
        for ((id, change) in changes) {
            val draft = proposal(id)
            repository.createProposal(if (id == "handed-off") draft.validate(CREATED) else draft,
                "create-$id")
            repository.changeProposal(id, 0, change, "change-$id",
                if (id == "expired") EXPIRY else CREATED + 1)
        }
        repository.appendActivity(ActivityRecord("standalone", ActivityEvent.PROFILE_ACTIVATED,
            CREATED, profileId = "personal"))
        val expected = repository.get()
        reopenDatabase()
        val actual = repository.get()
        assertResultsEqual(expected, actual)
        assertEquals(ProposalState.entries.toSet(),
            actual.proposals.map { it.proposal.state }.toSet())
        assertEquals(ProposalExplanation.USER_CORRECTED_FIELDS,
            actual.proposals.single { it.proposal.id == "corrected" }.proposal.explanation)
        assertEquals(setOf(CalendarField.TIME),
            actual.proposals.single { it.proposal.id == "review" }.proposal.ambiguousFields)
        assertEquals(expected.activity, repository.results.first().activity)
        assertEquals(null,
            actual.proposals.single { it.proposal.id == "no-expiry" }.proposal
                .expiresAtEpochMilliseconds)
    }

    @Test
    fun currentRevisionGuardsConcurrentCommandsAndBackwardsClocks(): Unit = runBlocking {
        repository.createProposal(proposal("concurrent"), "created")
        val second = RoomLocalResultsRepository(database)
        val start = CompletableDeferred<Unit>()
        val outcomes = coroutineScope {
            val first = async(Dispatchers.Default) {
                start.await()
                runCatching { repository.changeProposal("concurrent", 0, ProposalChange.Defer,
                    "defer", CREATED + 2) }
            }
            val other = async(Dispatchers.Default) {
                start.await()
                runCatching { second.changeProposal("concurrent", 0, ProposalChange.Fail,
                    "fail", CREATED + 2) }
            }
            start.complete(Unit)
            listOf(first.await(), other.await())
        }
        assertEquals(1, outcomes.count { it.isSuccess })
        assertTrue(outcomes.single { it.isFailure }.exceptionOrNull() is IllegalStateException)
        val expected = repository.get()
        assertEquals(1L, expected.proposals.single().revision)
        assertEquals(2, expected.activity.size)
        assertFailsWith<IllegalArgumentException> {
            repository.changeProposal("concurrent", 1, ProposalChange.Validate, "backwards",
                CREATED + 1)
        }
        assertFailsWith<IllegalStateException> { repository.deleteProposal("concurrent", 0) }
        assertResultsEqual(expected, repository.get())
    }

    @Test
    fun activityIdentifierCollisionRollsBackCreationAndStateChange(): Unit = runBlocking {
        repository.appendActivity(ActivityRecord("occupied", ActivityEvent.NOTIFICATION_IGNORED,
            CREATED))
        assertFailsWith<SQLiteConstraintException> {
            repository.createProposal(proposal("rolled-back"), "occupied")
        }
        assertTrue(repository.get().proposals.isEmpty())
        repository.createProposal(proposal("retained"), "created")
        val expected = repository.get()
        assertFailsWith<SQLiteConstraintException> {
            repository.changeProposal("retained", 0, ProposalChange.Validate, "occupied", CREATED)
        }
        assertResultsEqual(expected, repository.get())
        reopenDatabase()
        assertResultsEqual(expected, repository.get())
    }

    @Test
    fun observationNeverExposesProposalMutationWithoutItsActivity(): Unit = runBlocking {
        val snapshots = Channel<LocalResults>(Channel.UNLIMITED)
        val collector = launch(Dispatchers.Default) {
            repository.results.collect { snapshot ->
                for (stored in snapshot.proposals) {
                    assertTrue(snapshot.activity.any { it.proposalId == stored.proposal.id &&
                        it.event == ActivityEvent.CALENDAR_PROPOSAL_CREATED })
                    if (stored.revision == 1L) {
                        assertTrue(snapshot.activity.any { it.proposalId == stored.proposal.id &&
                            it.event == ActivityEvent.PROPOSAL_VALIDATED })
                    }
                }
                snapshots.send(snapshot)
            }
        }
        try {
            snapshots.awaitSnapshot { it.proposals.isEmpty() }
            repeat(5) { index ->
                repository.createProposal(proposal("flow-$index"), "create-$index")
                repository.changeProposal("flow-$index", 0, ProposalChange.Validate,
                    "validate-$index", CREATED + 1)
            }
            val complete = snapshots.awaitSnapshot {
                it.proposals.size == 5 && it.proposals.all { stored -> stored.revision == 1L }
            }
            assertEquals(10, complete.activity.size)
        } finally {
            collector.cancelAndJoin()
            snapshots.close()
        }
    }

    @Test
    fun clearingActivityAndDeletingProposalsAreIndependentOfConfiguration(): Unit = runBlocking {
        val configurationDatabase = LocalConfigurationDatabase.create(context)
        try {
            val configuration = RoomLocalConfigurationRepository(configurationDatabase)
            configuration.setPaused(true)
            configuration.setMonitoredApplications(setOf("synthetic.calendar"))
            repository.createProposal(proposal("delete"), "create-delete")
            repository.createProposal(proposal("keep"), "create-keep")
            val original = repository.get()
            val profile = configuration.getConfiguration().profiles.selectedProfile
            configuration.saveProfile(AutomationProfile(profile.id, "Renamed Synthetic Profile",
                profile.description, profile.automations))
            assertResultsEqual(original, repository.get())
            configuration.saveProfile(AutomationProfile("replacement", "Synthetic Replacement",
                automations = listOf(Automation("replacement-calendar", "Synthetic Automation",
                    true, NotificationSourceSelector.AnyMonitoredApplication,
                    AutomationAction.PROPOSE_CALENDAR_EVENT))))
            configuration.deleteProfile(profile.id, "replacement")
            reopenDatabase()
            assertResultsEqual(original, repository.get())
            repository.deleteProposal("delete", 0)
            assertEquals(original.activity, repository.get().activity)
            assertEquals(listOf("keep"), repository.get().proposals.map { it.proposal.id })
            val retained = repository.get().proposals.single()
            repository.clearActivity()
            assertTrue(repository.get().activity.isEmpty())
            assertStoredEqual(retained, repository.get().proposals.single())
            reopenDatabase()
            assertStoredEqual(retained, repository.get().proposals.single())
            assertTrue(repository.get().activity.isEmpty())
            val settings = configuration.getConfiguration()
            assertTrue(settings.isPaused)
            assertEquals(setOf("synthetic.calendar"), settings.monitoredApplicationIds)
            assertEquals("replacement", settings.profiles.selectedProfileId)
            assertEquals("Synthetic Replacement", settings.profiles.selectedProfile.name)
        } finally {
            configurationDatabase.close()
        }
    }

    @Test
    fun standaloneAppendCannotForgeLifecycleEventsOrProposalReferences(): Unit = runBlocking {
        for (event in ActivityEvent.entries.filter {
            it !in setOf(ActivityEvent.NOTIFICATION_IGNORED, ActivityEvent.PROFILE_ACTIVATED,
                ActivityEvent.NOTIFICATION_ACCESS_LOST)
        }) {
            assertFailsWith<IllegalArgumentException> {
                repository.appendActivity(ActivityRecord("forged", event, CREATED))
            }
        }
        assertFailsWith<IllegalArgumentException> {
            repository.appendActivity(ActivityRecord("forged", ActivityEvent.NOTIFICATION_IGNORED,
                CREATED, proposalId = "synthetic-proposal"))
        }
        assertTrue(repository.get().activity.isEmpty())
    }

    @Test
    fun unchangedExpiryCreatesNeitherRevisionNorActivity(): Unit = runBlocking {
        repository.createProposal(proposal("not-expired"), "created")
        val expected = repository.get()
        val unchanged = repository.changeProposal("not-expired", 0, ProposalChange.Expire,
            "unused", EXPIRY - 1)
        assertEquals(0L, unchanged.revision)
        assertResultsEqual(expected, repository.get())
    }

    @Test
    fun unknownStoredStateFailsReadsObservationAndMutationsWithoutReset(): Unit = runBlocking {
        repository.createProposal(proposal("corrupt"), "created")
        withContext(Dispatchers.IO) {
            database.openHelper.writableDatabase.execSQL(
                "UPDATE proposals SET state = ? WHERE id = ?",
                arrayOf("UNSUPPORTED_SYNTHETIC_STATE", "corrupt"),
            )
        }
        reopenDatabase()
        assertFailsWith<IllegalArgumentException> { repository.get() }
        assertFailsWith<IllegalArgumentException> {
            withTimeout(TIMEOUT_MILLIS) { repository.results.first() }
        }
        assertFailsWith<IllegalArgumentException> { repository.clearActivity() }
        assertFailsWith<IllegalArgumentException> { repository.deleteProposal("corrupt", 0) }
        assertFailsWith<IllegalArgumentException> {
            repository.createProposal(proposal("new"), "new-activity")
        }
        assertFailsWith<IllegalArgumentException> {
            repository.changeProposal("corrupt", 0, ProposalChange.Validate, "change", CREATED)
        }
        assertFailsWith<IllegalArgumentException> {
            repository.appendActivity(ActivityRecord("standalone", ActivityEvent.PROFILE_ACTIVATED,
                CREATED))
        }
        withContext(Dispatchers.IO) {
            database.openHelper.readableDatabase.query("SELECT state FROM proposals").use {
                assertTrue(it.moveToFirst())
                assertEquals("UNSUPPORTED_SYNTHETIC_STATE", it.getString(0))
                assertEquals(1, it.count)
            }
            database.openHelper.readableDatabase.query("SELECT id FROM activity").use {
                assertTrue(it.moveToFirst())
                assertEquals("created", it.getString(0))
            }
        }
    }

    @Test
    fun corruptDatesAmbiguityAndActivityTypesFailInsteadOfBeingNormalized(): Unit = runBlocking {
        repository.createProposal(proposal("invalid"), "created")
        val valid = repository.get().proposals.single().toEntity()
        val invalidRows = listOf(
            valid.copy(dateMonth = null),
            valid.copy(dateDay = 32),
            valid.copy(timeMinute = null),
            valid.copy(timeHour = 24),
            valid.copy(ambiguousFields = "DATE,DATE"),
            valid.copy(ambiguousFields = "UNSUPPORTED_FIELD"),
            valid.copy(explanation = "UNSUPPORTED_EXPLANATION"),
            valid.copy(revision = -1),
            valid.copy(updatedAtEpochMilliseconds = CREATED - 1),
            valid.copy(state = "READY", title = null),
        )
        for (invalid in invalidRows) {
            database.resultsDao().updateProposal(invalid)
            assertFailsWith<IllegalArgumentException> { repository.get() }
        }
        database.resultsDao().updateProposal(valid)
        withContext(Dispatchers.IO) {
            database.openHelper.writableDatabase.execSQL(
                "UPDATE activity SET event = 'UNSUPPORTED_SYNTHETIC_EVENT'",
            )
        }
        assertFailsWith<IllegalArgumentException> { repository.get() }
        assertFailsWith<IllegalArgumentException> { repository.clearActivity() }
    }

    @Test
    fun resultsDatabaseIsStoredSeparatelyInsideNoBackupDirectory(): Unit = runBlocking {
        repository.get()
        val path = withContext(Dispatchers.IO) {
            File(checkNotNull(database.openHelper.readableDatabase.path)).canonicalFile
        }
        assertEquals(context.noBackupFilesDir.canonicalFile, path.parentFile)
        assertEquals("local-results.db", path.name)
        assertTrue(path.isFile)
    }

    private fun reopenDatabase() {
        database.close()
        database = LocalResultsDatabase.create(context)
        repository = RoomLocalResultsRepository(database)
    }

    private fun fields(): CalendarFields = CalendarFields("Synthetic Event",
        CalendarDate(2026, 10, 4), CalendarTime(14, 30), "Synthetic Room A")

    private fun proposal(
        id: String,
        incomplete: Boolean = false,
        expiresAtEpochMilliseconds: Long? = EXPIRY,
    ): CalendarProposal =
        CalendarProposal.draft(
            id,
            ProposalOrigin("personal", "Synthetic Personal", "personal-calendar",
                "Synthetic Calendar Automation", "synthetic.calendar", "Synthetic Calendar"),
            if (incomplete) fields().copy(time = null) else fields(),
            CREATED,
            expiresAtEpochMilliseconds,
            if (incomplete) setOf(CalendarField.TIME) else emptySet(),
        )

    private fun assertResultsEqual(expected: LocalResults, actual: LocalResults) {
        assertEquals(expected.activity, actual.activity)
        assertEquals(expected.proposals.size, actual.proposals.size)
        expected.proposals.zip(actual.proposals).forEach { (first, second) ->
            assertStoredEqual(first, second)
        }
    }

    private fun assertStoredEqual(
        expected: StoredCalendarProposal,
        actual: StoredCalendarProposal,
    ) {
        assertEquals(expected.revision, actual.revision)
        assertEquals(expected.updatedAtEpochMilliseconds, actual.updatedAtEpochMilliseconds)
        assertEquals(expected.proposal.id, actual.proposal.id)
        assertEquals(expected.proposal.origin, actual.proposal.origin)
        assertEquals(expected.proposal.fields, actual.proposal.fields)
        assertEquals(expected.proposal.ambiguousFields, actual.proposal.ambiguousFields)
        assertEquals(expected.proposal.state, actual.proposal.state)
        assertEquals(expected.proposal.explanation, actual.proposal.explanation)
        assertEquals(expected.proposal.createdAtEpochMilliseconds,
            actual.proposal.createdAtEpochMilliseconds)
        assertEquals(expected.proposal.expiresAtEpochMilliseconds,
            actual.proposal.expiresAtEpochMilliseconds)
    }

    private suspend fun Channel<LocalResults>.awaitSnapshot(
        predicate: (LocalResults) -> Boolean,
    ): LocalResults = withTimeout(TIMEOUT_MILLIS) {
        var snapshot = receive()
        while (!predicate(snapshot)) snapshot = receive()
        snapshot
    }

    private companion object {
        const val CREATED = 100L
        const val EXPIRY = 1000L
        const val TIMEOUT_MILLIS = 5000L
    }
}
