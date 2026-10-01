package com.hayduck.notemate.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AndroidNotificationContentMapperTest {
    @Test
    fun readsOnlyTitleAndExpandedCurrentBody() {
        val keys = mutableListOf<String>()
        val content = AndroidNotificationContentMapper().map { key ->
            keys.add(key)
            when (key) {
                "android.title" -> "Synthetic title"
                "android.bigText" -> "Synthetic expanded event"
                else -> error("Unexpected content field")
            }
        }
        assertEquals(listOf("android.title", "android.bigText"), keys)
        assertEquals("Synthetic expanded event", content.body)
    }

    @Test
    fun fallsBackToCurrentTextWhenExpandedTextIsAbsent() {
        val content = AndroidNotificationContentMapper().map { key ->
            if (key == "android.text") "Synthetic current event" else null
        }
        assertNull(content.title)
        assertEquals("Synthetic current event", content.body)
    }

    @Test
    fun boundsAndroidInputBeforeCopying() {
        val content = AndroidNotificationContentMapper().map { "x".repeat(5000) }
        assertEquals(256, content.title?.length)
        assertEquals(4096, content.body?.length)
    }
}
