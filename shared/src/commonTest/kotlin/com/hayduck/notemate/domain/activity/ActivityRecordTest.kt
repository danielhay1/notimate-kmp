package com.hayduck.notemate.domain.activity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ActivityRecordTest {
    @Test
    fun activityContainsTypedOutcomeAndRedactedReferences() {
        val record = ActivityRecord("record", ActivityEvent.OPENED_IN_CALENDAR, 100,
            proposalId = "proposal", profileId = "profile")
        assertEquals(ActivityEvent.OPENED_IN_CALENDAR, record.event)
        assertEquals("ActivityRecord(event=OPENED_IN_CALENDAR, references=[REDACTED])",
            record.toString())
    }

    @Test
    fun blankIdentifiersAreRejectedWithoutEchoingInput() {
        assertFailsWith<IllegalArgumentException> {
            ActivityRecord("", ActivityEvent.PROFILE_ACTIVATED, 100)
        }
        assertFailsWith<IllegalArgumentException> {
            ActivityRecord("record", ActivityEvent.PROFILE_ACTIVATED, 100, profileId = " ")
        }
    }
}
