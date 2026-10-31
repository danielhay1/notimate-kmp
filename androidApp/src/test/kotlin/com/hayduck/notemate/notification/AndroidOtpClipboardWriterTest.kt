package com.hayduck.notemate.notification

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import com.hayduck.notemate.data.results.LocalResultsDatabase
import com.hayduck.notemate.data.results.RoomLocalResultsRepository
import com.hayduck.notemate.domain.activity.ActivityEvent
import com.hayduck.notemate.domain.activity.ActivityRecord
import com.hayduck.notemate.domain.extraction.OtpClipboardCopy
import com.hayduck.notemate.domain.proposal.ProposalOrigin
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AndroidOtpClipboardWriterTest {
    private lateinit var context: Context
    private lateinit var database: LocalResultsDatabase
    private lateinit var repository: RoomLocalResultsRepository
    private val copy = OtpClipboardCopy(
        code = "482910",
        origin = ProposalOrigin(
            profileId = "personal",
            profileName = "Personal",
            automationId = "otp-auto",
            automationName = "Copy OTP",
            applicationId = "synthetic.sms",
            applicationName = "Synthetic SMS",
        ),
        createdAtEpochMilliseconds = 500L,
    )

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
    fun copyWritesSensitivePrimaryClipAndRecordsContentFreeActivityOnly(): Unit = runBlocking {
        val writer = AndroidOtpClipboardWriter(context, repository, Dispatchers.Unconfined,
            nowEpochMilliseconds = { 500L }, isDeviceUnlocked = { true })
        assertTrue(writer.copyAndRecord(copy, "otp-copied-1"))

        val clipboard = checkNotNull(context.getSystemService(ClipboardManager::class.java))
        val primaryClip = assertNotNull(clipboard.primaryClip)
        assertEquals(AndroidOtpClipboardWriter.OTP_CLIP_LABEL, primaryClip.description.label)
        assertEquals("482910", primaryClip.getItemAt(0).text.toString())
        val extras = assertNotNull(primaryClip.description.extras)
        assertTrue(extras.getBoolean(AndroidOtpClipboardWriter.EXTRA_IS_SENSITIVE))

        val results = repository.get()
        assertTrue(results.proposals.isEmpty())
        assertEquals(
            listOf(ActivityRecord(
                id = "otp-copied-1",
                event = ActivityEvent.OTP_COPIED_TO_CLIPBOARD,
                occurredAtEpochMilliseconds = 500L,
                proposalId = null,
                profileId = "personal",
                automationId = "otp-auto",
                applicationId = "synthetic.sms",
            )),
            results.activity,
        )
        assertFalse(results.toString().contains("482910"))
    }

    @Test
    fun missingOrFailedClipboardNeverRecordsFalseActivityEvent(): Unit = runBlocking {
        val noClipboardContext = object : ContextWrapper(context) {
            override fun getSystemService(name: String): Any? =
                if (name == Context.CLIPBOARD_SERVICE) null else super.getSystemService(name)
        }
        val missingWriter = AndroidOtpClipboardWriter(
            noClipboardContext, repository, Dispatchers.Unconfined,
            nowEpochMilliseconds = { 500L }, isDeviceUnlocked = { true },
        )
        assertFalse(missingWriter.copyAndRecord(copy, "missing-clipboard"))
        assertTrue(repository.get().activity.isEmpty())

        val failingWriter = AndroidOtpClipboardWriter(
            context,
            repository,
            Dispatchers.Unconfined,
            nowEpochMilliseconds = { 500L }, isDeviceUnlocked = { true },
        ) { _, _ -> throw SecurityException("Clipboard write denied") }
        assertFalse(failingWriter.copyAndRecord(copy, "denied-clipboard"))
        assertTrue(repository.get().activity.isEmpty())
    }

    @Test
    fun cancellationAfterWriteStillRecordsExactlyOneAuditEvent(): Unit = runBlocking {
        val caller = Job()
        val writer = AndroidOtpClipboardWriter(
            context, repository, Dispatchers.Unconfined,
            nowEpochMilliseconds = { 500L }, isDeviceUnlocked = { true },
        ) { clipboard, clip ->
            clipboard.setPrimaryClip(clip)
            caller.cancel()
        }
        val operation = CoroutineScope(caller + Dispatchers.Unconfined).launch {
            writer.copyAndRecord(copy, "cancel-after-write")
        }
        operation.join()
        val clipboard = checkNotNull(context.getSystemService(ClipboardManager::class.java))
        assertEquals(copy.code, clipboard.primaryClip?.getItemAt(0)?.text.toString())
        assertEquals(listOf("cancel-after-write"), repository.get().activity.map { it.id })
    }

    @Test
    fun staleFutureAndLockedNotificationsNeverWriteOrAudit(): Unit = runBlocking {
        for ((now, unlocked) in listOf(60_501L to true, 499L to true, 500L to false)) {
            val writer = AndroidOtpClipboardWriter(
                context, repository, Dispatchers.Unconfined,
                nowEpochMilliseconds = { now }, isDeviceUnlocked = { unlocked },
            ) { _, _ -> error("Must not copy") }
            assertFalse(writer.copyAndRecord(copy, "rejected"))
        }
        assertTrue(repository.get().activity.isEmpty())
    }

    @Test
    fun alreadyCancelledCallerNeverWrites(): Unit = runBlocking {
        val caller = Job().apply { cancel() }
        val writer = AndroidOtpClipboardWriter(
            context, repository, Dispatchers.Unconfined,
            nowEpochMilliseconds = { 500L }, isDeviceUnlocked = { true },
        ) { _, _ -> error("Must not copy") }
        CoroutineScope(caller + Dispatchers.Unconfined).launch {
            writer.copyAndRecord(copy, "already-cancelled")
        }.join()
        assertTrue(repository.get().activity.isEmpty())
    }

    @Test
    fun blankActivityIdAndCancellationAreHandledExplicitly(): Unit = runBlocking {
        val writer = AndroidOtpClipboardWriter(
            context,
            repository,
            Dispatchers.Unconfined,
            nowEpochMilliseconds = { 500L }, isDeviceUnlocked = { true },
        ) { _, _ -> throw CancellationException() }
        assertFailsWith<IllegalArgumentException> { writer.copyAndRecord(copy, " ") }
        assertFailsWith<CancellationException> { writer.copyAndRecord(copy, "cancelled") }
        assertTrue(repository.get().activity.isEmpty())
    }
}
