package com.hayduck.notemate.domain.notification

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class NotificationContentMinimizerTest {
    @Test
    fun copiesOnlyBoundedCharactersWithoutStringifyingInput() {
        val value = object : CharSequence {
            override val length = 10000
            override fun get(index: Int): Char = 'x'
            override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = error("Copy")
            override fun toString(): String = error("Unbounded copy")
        }
        val content = NotificationContentMinimizer(3, 5).minimize(value, value)
        assertEquals("xxx", content.title)
        assertEquals("xxxxx", content.body)
    }

    @Test
    fun handlesMissingFieldsAndSurrogateBoundary() {
        val minimizer = NotificationContentMinimizer(2, 2)
        assertNull(minimizer.minimize(null, null).title)
        assertEquals("a", minimizer.minimize("a\uD83D\uDE00", null).title)
        assertFailsWith<IllegalArgumentException> { NotificationContentMinimizer(0, 2) }
    }
}
