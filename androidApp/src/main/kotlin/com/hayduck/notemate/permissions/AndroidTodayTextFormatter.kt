package com.hayduck.notemate.permissions

import android.content.Context
import android.icu.text.DateTimePatternGenerator
import android.icu.text.SimpleDateFormat
import android.icu.util.GregorianCalendar
import android.icu.util.TimeZone
import android.text.format.DateFormat
import com.hayduck.notemate.domain.proposal.CalendarDate
import com.hayduck.notemate.domain.proposal.CalendarTime
import com.hayduck.notemate.today.TodayTextFormatter
import java.util.Date

/** Formats wall-clock fields without resolving them into a device-zone instant. */
internal class AndroidTodayTextFormatter(context: Context) : TodayTextFormatter {
    private val context = context.applicationContext
    private val locale get() = context.resources.configuration.locales[0]

    override fun date(value: CalendarDate): String {
        val calendar = wallClockCalendar().apply {
            set(value.year, value.month - 1, value.day, 12, 0, 0)
        }
        return formatter("yMMMd", useUtc = true).format(calendar.time)
    }

    override fun time(value: CalendarTime): String {
        val calendar = wallClockCalendar().apply { set(2000, 0, 1, value.hour, value.minute, 0) }
        return formatter(timeSkeleton(), useUtc = true).format(calendar.time)
    }

    override fun timestamp(epochMilliseconds: Long): String =
        formatter("yMMMd" + timeSkeleton(), useUtc = false).format(Date(epochMilliseconds))

    private fun wallClockCalendar(): GregorianCalendar =
        GregorianCalendar(TimeZone.getTimeZone("UTC"), locale).apply {
            gregorianChange = Date(Long.MIN_VALUE)
            clear()
        }

    private fun timeSkeleton(): String = if (DateFormat.is24HourFormat(context)) "Hm" else "hm"

    private fun formatter(skeleton: String, useUtc: Boolean): SimpleDateFormat {
        val pattern = DateTimePatternGenerator.getInstance(locale).getBestPattern(skeleton)
        return SimpleDateFormat(pattern, locale).apply {
            if (useUtc) timeZone = TimeZone.getTimeZone("UTC")
            calendar = GregorianCalendar(timeZone, locale).apply {
                gregorianChange = Date(Long.MIN_VALUE)
            }
        }
    }
}
