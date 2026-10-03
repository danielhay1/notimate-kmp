package com.hayduck.notemate.featurekeys

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import note_mate.shared.generated.resources.Res
import note_mate.shared.generated.resources.feature_keys_continue
import note_mate.shared.generated.resources.feature_keys_default
import note_mate.shared.generated.resources.feature_keys_discarded
import note_mate.shared.generated.resources.feature_keys_intro
import note_mate.shared.generated.resources.feature_keys_invalid
import note_mate.shared.generated.resources.feature_keys_load_failed
import note_mate.shared.generated.resources.feature_keys_loading
import note_mate.shared.generated.resources.feature_keys_retry
import note_mate.shared.generated.resources.feature_keys_save_failed
import note_mate.shared.generated.resources.feature_keys_saving
import note_mate.shared.generated.resources.feature_keys_title
import note_mate.shared.generated.resources.feature_keys_value
import org.jetbrains.compose.resources.stringResource

/** Loads the app-owned manager before rendering the editor or the normal app destination. */
@Composable
fun FeatureKeyStartup(
    manager: FeatureKeyManager,
    showEditor: Boolean,
    readyContent: @Composable () -> Unit,
) {
    val model = viewModel {
        FeatureKeyStartupViewModel(manager, showEditor) {
            Res.readBytes("files/feature-keys.json").decodeToString()
        }
    }
    val state by model.state.collectAsStateWithLifecycle()
    MaterialTheme {
        when (val current = state) {
            FeatureKeyStartupUiState.Ready -> readyContent()
            is FeatureKeyStartupUiState.Editing -> FeatureKeyEditor(
                current, model::edit, model::continueToApp,
            )
            else -> Surface(Modifier.fillMaxSize()) {
                Column(
                    Modifier.safeDrawingPadding().padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (current == FeatureKeyStartupUiState.LoadFailed) {
                        Text(stringResource(Res.string.feature_keys_load_failed))
                        Button(onClick = model::load) {
                            Text(stringResource(Res.string.feature_keys_retry))
                        }
                    } else {
                        CircularProgressIndicator()
                        Text(stringResource(Res.string.feature_keys_loading))
                    }
                }
            }
        }
    }
}

@Composable
internal fun FeatureKeyEditor(
    state: FeatureKeyStartupUiState.Editing,
    onEdit: (String, String) -> Unit,
    onContinue: () -> Unit,
) {
    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.safeDrawingPadding().imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(Res.string.feature_keys_title),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(stringResource(Res.string.feature_keys_intro))
                            if (state.discardedOverrides) {
                                Text(stringResource(Res.string.feature_keys_discarded))
                            }
                        }
                    }
                    items(state.fields, key = { it.feature.key }) { field ->
                        FeatureKeyRow(field, !state.saving) { onEdit(field.feature.key, it) }
                    }
                }
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.saveFailed) {
                        Text(
                            stringResource(Res.string.feature_keys_save_failed),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Button(
                        onClick = onContinue,
                        enabled = state.canContinue,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(if (state.saving) {
                            Res.string.feature_keys_saving
                        } else {
                            Res.string.feature_keys_continue
                        }))
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureKeyRow(field: FeatureKeyField, enabled: Boolean, onEdit: (String) -> Unit) {
    val feature = field.feature
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(feature.key, style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(Res.string.feature_keys_default, feature.defaultValue.text),
            style = MaterialTheme.typography.bodyMedium,
        )
        if (feature.type == FeatureKeyType.BOOLEAN) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(field.text, Modifier.weight(1f))
                Switch(
                    checked = field.text == "true",
                    onCheckedChange = { onEdit(it.toString()) },
                    enabled = enabled,
                    modifier = Modifier.semantics { contentDescription = feature.key },
                )
            }
        } else {
            OutlinedTextField(
                value = field.text,
                onValueChange = onEdit,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.feature_keys_value, feature.type.jsonName)) },
                isError = !field.isValid,
                supportingText = if (!field.isValid) {
                    { Text(stringResource(Res.string.feature_keys_invalid, feature.type.jsonName)) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = when (feature.type) {
                    FeatureKeyType.INT -> KeyboardType.Number
                    FeatureKeyType.FLOAT, FeatureKeyType.DOUBLE -> KeyboardType.Decimal
                    else -> KeyboardType.Text
                }),
                singleLine = feature.type != FeatureKeyType.STRING,
            )
        }
    }
}
