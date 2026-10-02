package com.hayduck.notemate.domain.extraction

import com.hayduck.notemate.domain.proposal.CalendarDate
import com.hayduck.notemate.domain.proposal.CalendarTime

/**
 * Caller-resolved local date/time at notification posting, with its valid time zone and locale.
 * Relative-date interpretation is advisory; Android handoff still owns time-zone/DST validation.
 */
class NotificationInterpretationContext(
    val referenceDate: CalendarDate,
    val referenceTime: CalendarTime,
    val timeZoneId: String,
    val localeTag: String,
) {
    init {
        require(timeZoneId.matches(Regex("[A-Za-z0-9_+./:-]{1,100}")) &&
            localeTag.matches(Regex("[A-Za-z0-9-]{1,35}"))) { "Invalid interpretation context." }
    }

    override fun toString(): String = "NotificationInterpretationContext([REDACTED])"
}
