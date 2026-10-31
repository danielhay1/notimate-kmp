package com.hayduck.notemate.today

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.hayduck.notemate.designsystem.NotiMateTheme
import com.hayduck.notemate.domain.activity.ActivityEvent
import com.hayduck.notemate.domain.proposal.CalendarDate
import com.hayduck.notemate.domain.proposal.CalendarTime
import com.hayduck.notemate.domain.proposal.ProposalState
import note_mate.shared.generated.resources.Res
import note_mate.shared.generated.resources.preview_automation
import note_mate.shared.generated.resources.preview_profile
import note_mate.shared.generated.resources.preview_source
import note_mate.shared.generated.resources.preview_title
import note_mate.shared.generated.resources.today_preview_date
import note_mate.shared.generated.resources.today_preview_stamp
import note_mate.shared.generated.resources.today_preview_time
import org.jetbrains.compose.resources.stringResource

@Preview(name = "Today · light", group = "Today", widthDp = 360, heightDp = 1800)
@Composable
private fun TodayLightPreview() = TodayPreviewSurface { syntheticTodayState() }

@Preview(name = "Today · dark", group = "Today", widthDp = 360, heightDp = 1800)
@Composable
private fun TodayDarkPreview() = TodayPreviewSurface(dark = true) { syntheticTodayState() }

@Preview(name = "Today · Hebrew · 200%", group = "Today", locale = "he", widthDp = 320)
@Composable
private fun TodayHebrewLargePreview() = TodayPreviewSurface(rtl = true, fontScale = 2f) {
    syntheticTodayState()
}

@Preview(name = "Today · English · 200%", group = "Today", locale = "en", widthDp = 320)
@Composable
private fun TodayEnglishLargePreview() = TodayPreviewSurface(fontScale = 2f) {
    syntheticTodayState()
}

@Preview(name = "Today · Hebrew dark · 200%", group = "Today", locale = "he", widthDp = 320)
@Composable
private fun TodayHebrewDarkLargePreview() = TodayPreviewSurface(
    dark = true, rtl = true, fontScale = 2f,
) { syntheticTodayState() }

@Preview(name = "Today · wide", group = "Today", widthDp = 840, heightDp = 1400)
@Composable
private fun TodayWidePreview() = TodayPreviewSurface { syntheticTodayState() }

@Preview(name = "Today · empty and revoked", group = "Today", widthDp = 360)
@Composable
private fun TodayEmptyRevokedPreview() = TodayPreviewSurface {
    syntheticTodayState().copy(
        access = NotificationAccessState.REVOKED,
        results = TodayResultsUiState.Ready(emptyList(), emptyList()),
    )
}

@Preview(name = "Today · independent storage failures", group = "Today", widthDp = 360)
@Composable
private fun TodayStorageFailurePreview() = TodayPreviewSurface {
    syntheticTodayState().copy(
        configuration = TodayConfigurationUiState.Failed,
        results = TodayResultsUiState.Failed,
    )
}

@Preview(name = "Permission · welcome", group = "Permissions", widthDp = 360)
@Composable
private fun WelcomePreview() = TodayPreviewSurface {
    syntheticTodayState().copy(destination = TodayDestination.WELCOME)
}

@Preview(name = "Permission · access denied", group = "Permissions", widthDp = 360)
@Composable
private fun AccessDeniedPreview() = TodayPreviewSurface {
    syntheticTodayState().copy(
        destination = TodayDestination.ACCESS,
        access = NotificationAccessState.NOT_GRANTED,
    )
}

@Preview(
    name = "Permission · granted Hebrew dark", group = "Permissions", locale = "he", widthDp = 720,
)
@Composable
private fun AccessGrantedHebrewDarkPreview() = TodayPreviewSurface(dark = true, rtl = true) {
    syntheticTodayState().copy(destination = TodayDestination.ACCESS)
}

@Preview(name = "Permission · alerts denied", group = "Permissions", widthDp = 360)
@Composable
private fun AlertsDeniedPreview() = TodayPreviewSurface {
    syntheticTodayState().copy(
        destination = TodayDestination.ALERTS,
        alerts = ProposalAlertPermission.DENIED,
    )
}

@Preview(name = "Permission · settings unavailable", group = "Permissions", widthDp = 360)
@Composable
private fun SettingsUnavailablePreview() = TodayPreviewSurface {
    syntheticTodayState().copy(
        destination = TodayDestination.PERMISSIONS,
        message = TodayMessage.SETTINGS_UNAVAILABLE,
    )
}

@Composable
private fun TodayPreviewSurface(
    dark: Boolean = false,
    rtl: Boolean = false,
    fontScale: Float = 1f,
    state: @Composable () -> TodayUiState,
) {
    val density = LocalDensity.current
    val date = stringResource(Res.string.today_preview_date)
    val time = stringResource(Res.string.today_preview_time)
    val stamp = stringResource(Res.string.today_preview_stamp)
    val formatter = object : TodayTextFormatter {
        override fun date(value: CalendarDate): String = date
        override fun time(value: CalendarTime): String = time
        override fun timestamp(epochMilliseconds: Long): String = stamp
    }
    CompositionLocalProvider(
        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        LocalDensity provides Density(density.density, fontScale),
    ) {
        NotiMateTheme(darkTheme = dark) {
            TodayContent(state(), {}, formatter)
        }
    }
}

@Composable
private fun syntheticTodayState(): TodayUiState = TodayUiState(
    onboarding = OnboardingLoadState.READY,
    destination = TodayDestination.TODAY,
    access = NotificationAccessState.GRANTED,
    alerts = ProposalAlertPermission.NOT_REQUESTED,
    configuration = TodayConfigurationUiState.Ready(
        profileName = stringResource(Res.string.preview_profile),
        isPaused = false,
        monitoredSourceCount = 0,
        enabledAutomationCount = 1,
    ),
    results = TodayResultsUiState.Ready(
        proposals = listOf(TodayProposalUiState(
            id = "sample-proposal",
            title = stringResource(Res.string.preview_title),
            date = CalendarDate(2026, 10, 20),
            time = null,
            location = null,
            sourceName = stringResource(Res.string.preview_source),
            profileName = stringResource(Res.string.preview_profile),
            automationName = stringResource(Res.string.preview_automation),
            state = ProposalState.NEEDS_REVIEW,
        )),
        recentOutcomes = listOf(RecentOutcomeUiState(
            id = "sample-outcome",
            event = ActivityEvent.CALENDAR_PROPOSAL_CREATED,
            occurredAtEpochMilliseconds = 1792519200000,
        )),
    ),
)
