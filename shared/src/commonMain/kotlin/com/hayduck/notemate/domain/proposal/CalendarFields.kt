package com.hayduck.notemate.domain.proposal

/** Local calendar date, validated independently of platform date libraries. */
data class CalendarDate(val year: Int, val month: Int, val day: Int) {
    init {
        require(year in 1..9999 && month in 1..12) { "Invalid calendar date." }
        val leapYear = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
        val days = when (month) {
            2 -> if (leapYear) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        require(day in 1..days) { "Invalid calendar date." }
    }
}

/** Local wall-clock time; the Android handoff adapter owns time-zone resolution. */
data class CalendarTime(val hour: Int, val minute: Int) {
    init {
        require(hour in 0..23 && minute in 0..59) { "Invalid calendar time." }
    }
}

/** Editable structured event fields, never a copy of the source notification. */
data class CalendarFields(
    val title: String?,
    val date: CalendarDate?,
    val time: CalendarTime?,
    val location: String? = null,
) {
    override fun toString(): String = "CalendarFields([REDACTED])"
}

enum class CalendarField { TITLE, DATE, TIME, LOCATION }

enum class ProposalIssue { MISSING_TITLE, MISSING_DATE, MISSING_TIME, AMBIGUOUS_FIELDS }

/** Returns typed issues without retaining or interpolating extracted text. */
fun CalendarFields.validationIssues(
    ambiguousFields: Set<CalendarField> = emptySet(),
): Set<ProposalIssue> = buildSet {
    if (title.isNullOrBlank()) add(ProposalIssue.MISSING_TITLE)
    if (date == null) add(ProposalIssue.MISSING_DATE)
    if (time == null) add(ProposalIssue.MISSING_TIME)
    if (ambiguousFields.isNotEmpty()) add(ProposalIssue.AMBIGUOUS_FIELDS)
}
