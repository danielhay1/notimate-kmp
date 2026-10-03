package com.hayduck.notemate.featurekeys

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class NotificationTestViewModel(private val sender: AndroidTestNotificationSender) :
    ViewModel() {
    private val mutableState = MutableStateFlow(NotificationTestUiState())
    val state = mutableState.asStateFlow()

    fun editTitle(title: String) {
        mutableState.value = mutableState.value.copy(title = title, result = null)
    }

    fun editBody(body: String) {
        mutableState.value = mutableState.value.copy(body = body, result = null)
    }

    fun send(): NotificationTestResult {
        val result = sender.send(mutableState.value)
        mutableState.value = mutableState.value.copy(result = result)
        return result
    }

    fun permissionResult(granted: Boolean) {
        mutableState.value = mutableState.value.copy(result = if (granted) {
            NotificationTestResult.PERMISSION_REQUIRED
        } else {
            NotificationTestResult.BLOCKED
        })
    }
}
