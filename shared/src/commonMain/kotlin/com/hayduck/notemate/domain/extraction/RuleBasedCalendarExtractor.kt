package com.hayduck.notemate.domain.extraction

import com.hayduck.notemate.domain.notification.ObservedNotification
import com.hayduck.notemate.domain.proposal.CalendarField
import com.hayduck.notemate.domain.proposal.CalendarFields

/** Parses English labeled blocks; relative dates, time zones, and narrative are unsupported. */
class RuleBasedCalendarExtractor : NotificationExtractor {
    override val requiresHeavyWork: Boolean = false

    override suspend fun extract(
        notification: ObservedNotification,
        context: NotificationInterpretationContext?,
    ): ExtractionResult {
        if (!notification.isWithinExtractionLimits()) {
            return ExtractionResult.ReviewNeeded(ExtractionReviewReason.INPUT_LIMIT)
        }
        val lines = listOfNotNull(notification.content.title, notification.content.body)
            .flatMap { it.split('\n', ';') }.map(String::trim).filter(String::isNotEmpty)
        if (lines.isEmpty()) return ExtractionResult.Ignored
        val labels = lines.map { line ->
            val separator = line.indexOf(':')
            if (separator < 0) "" to line
            else line.substring(0, separator).trim().lowercase() to
                line.substring(separator + 1).trim()
        }
        if (labels.none { it.first in eventLabels }) return ExtractionResult.Unknown
        if (labels.any { it.first !in supportedLabels }) {
            return ExtractionResult.ReviewNeeded(ExtractionReviewReason.UNSUPPORTED_FORMAT)
        }
        val ambiguous = mutableSetOf<CalendarField>()
        fun value(field: CalendarField, acceptedLabels: Set<String>): String? {
            val values = labels.filter { it.first in acceptedLabels }.map { it.second }
            if (values.size > 1) {
                ambiguous.add(field)
                return null
            }
            return values.singleOrNull()?.takeIf { it.isNotBlank() }
        }
        val title = value(CalendarField.TITLE, eventLabels)
        val dateText = value(CalendarField.DATE, setOf("date"))
        val timeText = value(CalendarField.TIME, setOf("time"))
        val date = dateText?.let(::parseCalendarDate)
        val time = timeText?.let(::parseCalendarTime)
        if (dateText != null && date == null) ambiguous.add(CalendarField.DATE)
        if (timeText != null && time == null) ambiguous.add(CalendarField.TIME)
        val extraction = CalendarExtraction(
            CalendarFields(title, date, time, value(CalendarField.LOCATION, setOf("location"))),
            ambiguous,
        )
        return if (extraction.isSchemaValid()) ExtractionResult.Calendar(extraction)
        else ExtractionResult.ReviewNeeded(ExtractionReviewReason.INVALID_OUTPUT)
    }

    private companion object {
        val eventLabels = setOf("event", "meeting", "appointment")
        val supportedLabels = eventLabels + setOf("date", "time", "location")
    }
}
