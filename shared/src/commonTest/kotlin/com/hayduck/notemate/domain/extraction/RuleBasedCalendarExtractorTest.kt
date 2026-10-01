package com.hayduck.notemate.domain.extraction

import com.hayduck.notemate.domain.notification.NotificationContent
import com.hayduck.notemate.domain.notification.NotificationSource
import com.hayduck.notemate.domain.notification.ObservedNotification
import com.hayduck.notemate.domain.proposal.CalendarDate
import com.hayduck.notemate.domain.proposal.CalendarField
import com.hayduck.notemate.domain.proposal.CalendarTime
import com.hayduck.notemate.domain.proposal.ProposalIssue
import com.hayduck.notemate.domain.proposal.validationIssues
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class RuleBasedCalendarExtractorTest {
    private val extractor = RuleBasedCalendarExtractor()

    @Test
    fun explicitEventBlockExtractsOnlyStructuredFields() {
        val candidate = calendar("Meeting: Synthetic planning",
            "Date: 2028-02-29; Time: 14:30; Location: Synthetic room")
        assertEquals("Synthetic planning", candidate.fields.title)
        assertEquals(CalendarDate(2028, 2, 29), candidate.fields.date)
        assertEquals(CalendarTime(14, 30), candidate.fields.time)
        assertEquals("Synthetic room", candidate.fields.location)
        assertEquals(emptySet(), candidate.fields.validationIssues(candidate.ambiguousFields))
        assertFalse(candidate.toString().contains("Synthetic planning"))
    }

    @Test
    fun missingFieldsRemainExplicitlyIncomplete() {
        val candidate = calendar(null, "Event: Synthetic demo")
        assertEquals(setOf(ProposalIssue.MISSING_DATE, ProposalIssue.MISSING_TIME),
            candidate.fields.validationIssues(candidate.ambiguousFields))
    }

    @Test
    fun invalidOrUnsupportedDatesAndTimesAreNeverGuessed() {
        listOf("2027-02-29", "0000-01-01", "2028-13-01", "02/03/2028", "tomorrow").forEach {
            val candidate = calendar("Appointment: Synthetic check", "Date: $it; Time: 24:00")
            assertEquals(null, candidate.fields.date)
            assertEquals(null, candidate.fields.time)
            assertEquals(setOf(CalendarField.DATE, CalendarField.TIME), candidate.ambiguousFields)
        }
        val candidate = calendar("Event: Synthetic demo", "Date: 2028-01-01; Time: 2:30 PM")
        assertEquals(null, candidate.fields.time)
        assertEquals(setOf(CalendarField.TIME), candidate.ambiguousFields)
    }

    @Test
    fun repeatedFieldsNeverChooseAnArbitraryValue() {
        val candidate = calendar("Meeting: Synthetic first", "Event: Synthetic second; " +
            "Date: 2028-01-01; Date: 2028-01-02; Time: 14:30; Location: A; Location: B")
        assertEquals(null, candidate.fields.title)
        assertEquals(null, candidate.fields.date)
        assertEquals(null, candidate.fields.location)
        assertEquals(setOf(CalendarField.TITLE, CalendarField.DATE, CalendarField.LOCATION),
            candidate.ambiguousFields)
    }

    @Test
    fun narrativeTimeZonesAndUnrelatedContentUseFallback() {
        assertEquals(ExtractionResult.Unknown, extractor.extract(notification("Synthetic message")))
        listOf("Timezone: UTC", "Tomorrow works", "Duration: 30").forEach {
            assertEquals(ExtractionResult.ReviewNeeded(ExtractionReviewReason.UNSUPPORTED_FORMAT),
                extractor.extract(notification("Event: Synthetic demo", it)))
        }
        assertEquals(ExtractionResult.Ignored, extractor.extract(notification(" ", "\n")))
    }

    @Test
    fun limitsAndControlCharactersFailWithoutLeakingContent() {
        assertEquals(ExtractionResult.ReviewNeeded(ExtractionReviewReason.INPUT_LIMIT),
            extractor.extract(notification("a".repeat(257))))
        assertEquals(ExtractionResult.ReviewNeeded(ExtractionReviewReason.INPUT_LIMIT),
            extractor.extract(notification(null, "a".repeat(4097))))
        assertEquals(ExtractionResult.ReviewNeeded(ExtractionReviewReason.INVALID_OUTPUT),
            extractor.extract(notification("Event: Synthetic\u0001demo")))
    }

    private fun calendar(title: String?, body: String?): CalendarExtraction =
        assertIs<ExtractionResult.Calendar>(extractor.extract(notification(title, body))).extraction

    private fun notification(title: String?, body: String? = null): ObservedNotification =
        ObservedNotification(NotificationSource("synthetic.app"), 100,
            NotificationContent(title, body))
}
