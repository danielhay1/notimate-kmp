package com.hayduck.notemate.featurekeys

enum class NotificationTestResult {
    POSTED,
    INVALID,
    PERMISSION_REQUIRED,
    BLOCKED,
    FAILED,
    DISABLED,
}

/** Transient synthetic input only; never persist this form or include its content in diagnostics. */
data class NotificationTestUiState(
    val title: String = "",
    val body: String = "",
    val result: NotificationTestResult? = null,
) {
    val isValid: Boolean get() = title.isNotBlank() && title.length <= 256 &&
        body.isNotBlank() && body.length <= 4096

    override fun toString(): String = "NotificationTestUiState(content=<redacted>, result=$result)"
}
