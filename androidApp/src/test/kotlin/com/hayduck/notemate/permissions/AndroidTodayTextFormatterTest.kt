package com.hayduck.notemate.permissions

import android.app.Application
import android.provider.Settings
import com.hayduck.notemate.domain.proposal.CalendarDate
import com.hayduck.notemate.domain.proposal.CalendarTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "en")
class AndroidTodayTextFormatterTest {
    @Test
    fun structuredFieldsRespectLocaleAndUserClockWithoutWallTimeConversion() {
        val context = RuntimeEnvironment.getApplication()
        val formatter = AndroidTodayTextFormatter(context)
        Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, "24")
        assertEquals("Jan 2, 2027", formatter.date(CalendarDate(2027, 1, 2)))
        assertEquals("18:05", formatter.time(CalendarTime(18, 5)))
        Settings.System.putString(context.contentResolver, Settings.System.TIME_12_24, "12")
        assertEquals("6:05 PM", formatter.time(CalendarTime(18, 5)))
        assertTrue(formatter.timestamp(1_000_000_000).contains("1970"))
    }

    @Test
    @Config(qualifiers = "he")
    fun hebrewDatesUseLocalizedMonthAndPreserveDate() {
        val formatter = AndroidTodayTextFormatter(RuntimeEnvironment.getApplication())
        val date = formatter.date(CalendarDate(2027, 1, 2))
        assertTrue(date.contains("2027"))
        assertTrue(date.any { it in '\u0590'..'\u05FF' })
    }
}
