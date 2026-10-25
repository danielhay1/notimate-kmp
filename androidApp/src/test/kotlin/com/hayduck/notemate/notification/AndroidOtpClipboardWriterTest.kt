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
import kotlinx.coroutines.Dispatchers
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
        val writer = AndroidOtpClipboardWriter(context, repository, Dispatchers.Unconfined)
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
        )
        assertFalse(missingWriter.copyAndRecord(copy, "missing-clipboard"))
        assertTrue(repository.get().activity.isEmpty())

        val failingWriter = AndroidOtpClipboardWriter(
            context,
            repository,
            Dispatchers.Unconfined,
        ) { _, _ -> throw SecurityException("Clipboard write denied") }
        assertFalse(failingWriter.copyAndRecord(copy, "denied-clipboard"))
        assertTrue(repository.get().activity.isEmpty())
    }

    @Test
    fun blankActivityIdAndCancellationAreHandledExplicitly(): Unit = runBlocking {
        val writer = AndroidOtpClipboardWriter(
            context,
            repository,
            Dispatchers.Unconfined,
        ) { _, _ -> throw CancellationException() }
        assertFailsWith<IllegalArgumentException> { writer.copyAndRecord(copy, " ") }
        assertFailsWith<CancellationException> { writer.copyAndRecord(copy, "cancelled") }
        assertTrue(repository.get().activity.isEmpty())
    }
}
