package com.hayduck.notemate.domain.extraction

import com.hayduck.notemate.domain.proposal.CalendarDate
import com.hayduck.notemate.domain.proposal.CalendarTime

private val datePattern = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")
private val timePattern = Regex("[0-9]{2}:[0-9]{2}")

internal fun parseCalendarDate(text: String): CalendarDate? {
    if (!datePattern.matches(text)) return null
    return try {
        CalendarDate(text.substring(0, 4).toInt(), text.substring(5, 7).toInt(),
            text.substring(8, 10).toInt())
    } catch (_: IllegalArgumentException) {
        null
    }
}

internal fun parseCalendarTime(text: String): CalendarTime? {
    if (!timePattern.matches(text)) return null
    return try {
        CalendarTime(text.substring(0, 2).toInt(), text.substring(3, 5).toInt())
    } catch (_: IllegalArgumentException) {
        null
    }
}
