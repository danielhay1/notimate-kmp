package com.hayduck.notemate.today

import com.hayduck.notemate.domain.proposal.CalendarDate
import com.hayduck.notemate.domain.proposal.CalendarTime

/** Locale-aware display only; proposal wall-clock fields are not resolved into instants here. */
interface TodayTextFormatter {
    fun date(value: CalendarDate): String
    fun time(value: CalendarTime): String
    fun timestamp(epochMilliseconds: Long): String
}
