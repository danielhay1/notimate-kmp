package com.hayduck.notemate.featurekeys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeatureKeyField(val feature: FeatureKey, val text: String, val isValid: Boolean = true)

sealed interface FeatureKeyStartupUiState {
    data object Loading : FeatureKeyStartupUiState
    data object LoadFailed : FeatureKeyStartupUiState
    data object Ready : FeatureKeyStartupUiState
    data class Editing(
        val fields: List<FeatureKeyField>,
        val discardedOverrides: Boolean,
        val saving: Boolean = false,
        val saveFailed: Boolean = false,
    ) : FeatureKeyStartupUiState {
        val canContinue: Boolean get() = !saving && fields.all { it.isValid }
    }
}

/** Owns startup loading and a draft; edits become app configuration only after a successful save. */
class FeatureKeyStartupViewModel(
    private val manager: FeatureKeyManager,
    private val showEditor: Boolean,
    private val loadDefaults: suspend () -> String,
) : ViewModel() {
    private val mutableState = MutableStateFlow<FeatureKeyStartupUiState>(
        FeatureKeyStartupUiState.Loading,
    )
    val state = mutableState.asStateFlow()

    init { load() }

    fun load() {
        if (mutableState.value is FeatureKeyStartupUiState.Editing ||
            mutableState.value is FeatureKeyStartupUiState.Ready
        ) return
        mutableState.value = FeatureKeyStartupUiState.Loading
        viewModelScope.launch {
            try {
                if (!manager.isInitialized) manager.initialize(loadDefaults())
                mutableState.value = if (manager.isDebug && showEditor) {
                    FeatureKeyStartupUiState.Editing(
                        manager.keys.value.map { FeatureKeyField(it, it.value.text) },
                        manager.discardedOverrides,
                    )
                } else {
                    FeatureKeyStartupUiState.Ready
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = FeatureKeyStartupUiState.LoadFailed
            }
        }
    }

    fun edit(key: String, text: String) {
        val current = mutableState.value as? FeatureKeyStartupUiState.Editing ?: return
        if (current.saving) return
        mutableState.value = current.copy(
            fields = current.fields.map { field ->
                if (field.feature.key != key) field else field.copy(
                    text = text,
                    isValid = parseFeatureKeyValue(field.feature.type, text) != null,
                )
            },
            saveFailed = false,
        )
    }

    fun continueToApp() {
        val current = mutableState.value as? FeatureKeyStartupUiState.Editing ?: return
        if (!current.canContinue) return
        mutableState.value = current.copy(saving = true, saveFailed = false)
        viewModelScope.launch {
            try {
                manager.applyEdits(current.fields.associate { it.feature.key to it.text })
                mutableState.value = FeatureKeyStartupUiState.Ready
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = current.copy(saveFailed = true)
            }
        }
    }
}
