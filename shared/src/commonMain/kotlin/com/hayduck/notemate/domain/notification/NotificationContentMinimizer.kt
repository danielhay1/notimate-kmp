package com.hayduck.notemate.domain.notification

/** Copies only visible title and body, stripping spans and omitting history and other extras. */
class NotificationContentMinimizer(
    private val maximumTitleLength: Int,
    private val maximumBodyLength: Int,
) {
    init {
        require(maximumTitleLength > 0 && maximumBodyLength > 0)
    }

    fun minimize(title: CharSequence?, body: CharSequence?): NotificationContent =
        NotificationContent(copyBounded(title, maximumTitleLength), copyBounded(body, maximumBodyLength))

    private fun copyBounded(value: CharSequence?, limit: Int): String? {
        if (value == null) return null
        var count = minOf(value.length, limit)
        if (count > 0 && count < value.length && value[count - 1].isHighSurrogate()) count--
        return buildString(count) {
            for (index in 0 until count) append(value[index])
        }
    }
}
