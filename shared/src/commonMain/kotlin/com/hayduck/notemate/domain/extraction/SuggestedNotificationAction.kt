package com.hayduck.notemate.domain.extraction

/** Typed advisory action payloads; each new action requires its own schema and policy handling. */
sealed interface SuggestedNotificationAction {
    class CalendarEvent(val calendar: CalendarExtraction) : SuggestedNotificationAction {
        override fun toString(): String = "CalendarEvent([REDACTED])"
    }

    class OtpCode(val otp: OtpExtraction) : SuggestedNotificationAction {
        override fun toString(): String = "OtpCode([REDACTED])"
    }
}
