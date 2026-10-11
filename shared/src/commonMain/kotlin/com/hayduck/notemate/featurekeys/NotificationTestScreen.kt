package com.hayduck.notemate.featurekeys

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import note_mate.shared.generated.resources.Res
import note_mate.shared.generated.resources.notification_testing_back
import note_mate.shared.generated.resources.notification_testing_blocked
import note_mate.shared.generated.resources.notification_testing_body_field
import note_mate.shared.generated.resources.notification_testing_disabled
import note_mate.shared.generated.resources.notification_testing_failed
import note_mate.shared.generated.resources.notification_testing_intro
import note_mate.shared.generated.resources.notification_testing_invalid
import note_mate.shared.generated.resources.notification_testing_permission
import note_mate.shared.generated.resources.notification_testing_send
import note_mate.shared.generated.resources.notification_testing_sent
import note_mate.shared.generated.resources.notification_testing_settings
import note_mate.shared.generated.resources.notification_testing_title
import note_mate.shared.generated.resources.notification_testing_title_field
import org.jetbrains.compose.resources.stringResource

/** Renders transient input; the platform owner handles posting, permissions, settings, and Back. */
@Composable
fun NotificationTestScreen(
    state: NotificationTestUiState,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onSend: () -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
) {
    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            Box(
                Modifier.safeDrawingPadding().imePadding(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    Modifier.widthIn(max = 720.dp).fillMaxWidth()
                        .verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TextButton(onClick = onBack) {
                        Text(stringResource(Res.string.notification_testing_back))
                    }
                    Text(
                        stringResource(Res.string.notification_testing_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(stringResource(Res.string.notification_testing_intro))
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = onTitleChange,
                        label = { Text(stringResource(Res.string.notification_testing_title_field)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = state.title.length > 256,
                    )
                    OutlinedTextField(
                        value = state.body,
                        onValueChange = onBodyChange,
                        label = { Text(stringResource(Res.string.notification_testing_body_field)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        isError = state.body.length > 4096,
                    )
                    if (!state.isValid) Text(stringResource(Res.string.notification_testing_invalid))
                    state.result?.let { result ->
                        Text(stringResource(when (result) {
                            NotificationTestResult.POSTED -> Res.string.notification_testing_sent
                            NotificationTestResult.INVALID -> Res.string.notification_testing_invalid
                            NotificationTestResult.PERMISSION_REQUIRED ->
                                Res.string.notification_testing_permission
                            NotificationTestResult.BLOCKED -> Res.string.notification_testing_blocked
                            NotificationTestResult.FAILED -> Res.string.notification_testing_failed
                            NotificationTestResult.DISABLED -> Res.string.notification_testing_disabled
                        }))
                    }
                    Button(
                        onClick = onSend,
                        enabled = state.isValid,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.notification_testing_send))
                    }
                    TextButton(onClick = onSettings) {
                        Text(stringResource(Res.string.notification_testing_settings))
                    }
                }
            }
        }
    }
}
