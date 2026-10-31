package com.hayduck.notemate.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import com.hayduck.notemate.designsystem.NotiMateShapes
import com.hayduck.notemate.designsystem.NotiMateSpacing
import com.hayduck.notemate.designsystem.NotiMateTheme
import note_mate.shared.generated.resources.Res
import note_mate.shared.generated.resources.today_action_failed
import note_mate.shared.generated.resources.today_close_message
import note_mate.shared.generated.resources.today_loading
import note_mate.shared.generated.resources.today_local_only
import note_mate.shared.generated.resources.today_onboarding_failed
import note_mate.shared.generated.resources.today_permission_check_failed
import note_mate.shared.generated.resources.today_retry
import note_mate.shared.generated.resources.today_save_failed
import note_mate.shared.generated.resources.today_settings_unavailable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Renders local state; the caller owns permission launches and durable writes. */
@Composable
fun TodayApp(
    state: TodayUiState,
    onAction: (TodayAction) -> Unit,
    formatter: TodayTextFormatter,
    onOpenNotificationTesting: (() -> Unit)? = null,
) {
    NotiMateTheme {
        TodayContent(state, onAction, formatter, onOpenNotificationTesting)
    }
}

@Composable
internal fun TodayContent(
    state: TodayUiState,
    onAction: (TodayAction) -> Unit,
    formatter: TodayTextFormatter,
    onOpenNotificationTesting: (() -> Unit)? = null,
) {
    val window = currentWindowAdaptiveInfoV2().windowSizeClass
    val wide = window.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)
    val inset = if (wide) NotiMateSpacing.space24 else NotiMateSpacing.space16
    val todayScroll = rememberLazyListState()
    val setupScroll = rememberLazyListState()
    LaunchedEffect(state.destination) {
        if (state.destination != TodayDestination.TODAY) setupScroll.scrollToItem(0)
    }
    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.safeDrawingPadding().fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                state = if (state.destination == TodayDestination.TODAY) {
                    todayScroll
                } else {
                    setupScroll
                },
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
                contentPadding = PaddingValues(inset),
                verticalArrangement = Arrangement.spacedBy(NotiMateSpacing.space24),
            ) {
                if (state.message != null) {
                    item(key = "message") {
                        TodayMessagePanel(
                            message = when (state.message) {
                                TodayMessage.PERMISSION_CHECK_FAILED ->
                                    Res.string.today_permission_check_failed
                                TodayMessage.SAVE_FAILED -> Res.string.today_save_failed
                                TodayMessage.ACTION_FAILED -> Res.string.today_action_failed
                                TodayMessage.SETTINGS_UNAVAILABLE ->
                                    Res.string.today_settings_unavailable
                            },
                            actionLabel = Res.string.today_close_message,
                            onAction = { onAction(TodayAction.DISMISS_MESSAGE) },
                        )
                    }
                }
                when (state.onboarding) {
                    OnboardingLoadState.LOADING -> item { TodayLoading() }
                    OnboardingLoadState.FAILED -> item {
                        TodayMessagePanel(
                            Res.string.today_onboarding_failed,
                            Res.string.today_retry,
                        ) { onAction(TodayAction.RETRY_ONBOARDING) }
                    }
                    OnboardingLoadState.READY -> when (state.destination) {
                        TodayDestination.TODAY -> todayFeed(
                            state, onAction, formatter, onOpenNotificationTesting,
                        )
                        else -> item {
                            PermissionOnboardingContent(state, onAction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun TodayHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
internal fun TodayLoading() {
    Column(verticalArrangement = Arrangement.spacedBy(NotiMateSpacing.space12)) {
        CircularProgressIndicator()
        Text(stringResource(Res.string.today_loading))
    }
}

@Composable
internal fun TodayMessagePanel(
    message: StringResource,
    actionLabel: StringResource,
    onAction: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = NotiMateShapes.card,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(NotiMateSpacing.space16),
            verticalArrangement = Arrangement.spacedBy(NotiMateSpacing.space12),
        ) {
            Text(stringResource(message))
            TextButton(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth().heightIn(min = NotiMateSpacing.space48),
            ) { Text(stringResource(actionLabel)) }
        }
    }
}

@Composable
internal fun TodayActionButton(
    text: StringResource,
    enabled: Boolean = true,
    secondary: Boolean = false,
    onAction: () -> Unit,
) {
    val modifier = Modifier.fillMaxWidth().heightIn(min = NotiMateSpacing.space48)
    if (secondary) {
        TextButton(onClick = onAction, enabled = enabled, modifier = modifier) {
            Text(stringResource(text))
        }
    } else {
        Button(onClick = onAction, enabled = enabled, modifier = modifier) {
            Text(stringResource(text))
        }
    }
}

@Composable
internal fun PrivacyIndicator() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = NotiMateShapes.compactPanel,
    ) {
        Text(
            stringResource(Res.string.today_local_only),
            Modifier.fillMaxWidth().padding(NotiMateSpacing.space16),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
