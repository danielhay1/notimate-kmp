package com.hayduck.notemate.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import note_mate.shared.generated.resources.Res
import note_mate.shared.generated.resources.preview_automation
import note_mate.shared.generated.resources.preview_deferred
import note_mate.shared.generated.resources.preview_degraded
import note_mate.shared.generated.resources.preview_expired
import note_mate.shared.generated.resources.preview_failed
import note_mate.shared.generated.resources.preview_needs_review
import note_mate.shared.generated.resources.preview_profile
import note_mate.shared.generated.resources.preview_ready
import note_mate.shared.generated.resources.preview_source
import note_mate.shared.generated.resources.preview_summary
import note_mate.shared.generated.resources.preview_title
import note_mate.shared.generated.resources.preview_unavailable
import org.jetbrains.compose.resources.stringResource

@Preview(name = "Processing · light", group = "Processing", widthDp = 360, heightDp = 1100)
@Composable
private fun ProcessingLightPreview() = PreviewSurface { ProcessingPreviewStates() }

@Preview(name = "Processing · dark", group = "Processing", widthDp = 360, heightDp = 1100)
@Composable
private fun ProcessingDarkPreview() = PreviewSurface(dark = true) { ProcessingPreviewStates() }

@Preview(name = "Processing · Hebrew · 200%", group = "Processing", locale = "he", widthDp = 320)
@Composable
private fun ProcessingHebrewLargePreview() = PreviewSurface(rtl = true, fontScale = 2f) {
    ProcessingPreviewStates()
}

@Preview(
    name = "Processing · Hebrew dark · 200%", group = "Processing", locale = "he", widthDp = 320,
)
@Composable
private fun ProcessingHebrewDarkLargePreview() = PreviewSurface(
    dark = true, rtl = true, fontScale = 2f,
) { ProcessingPreviewStates() }

@Preview(name = "Processing · English · 200%", group = "Processing", locale = "en", widthDp = 320)
@Composable
private fun ProcessingEnglishLargePreview() = PreviewSurface(fontScale = 2f) {
    ProcessingPreviewStates()
}

@Preview(name = "Processing · wide", group = "Processing", widthDp = 720)
@Composable
private fun ProcessingWidePreview() = PreviewSurface { ProcessingPreviewStates() }

@Preview(name = "Proposals · light", group = "Proposal", widthDp = 360, heightDp = 2600)
@Composable
private fun ProposalLightPreview() = PreviewSurface { ProposalPreviewStates() }

@Preview(name = "Proposals · dark", group = "Proposal", widthDp = 360, heightDp = 2600)
@Composable
private fun ProposalDarkPreview() = PreviewSurface(dark = true) { ProposalPreviewStates() }

@Preview(name = "Proposals · Hebrew · 200%", group = "Proposal", locale = "he", widthDp = 320)
@Composable
private fun ProposalHebrewLargePreview() = PreviewSurface(rtl = true, fontScale = 2f) {
    ProposalPreviewStates()
}

@Preview(name = "Proposals · Hebrew dark · 200%", group = "Proposal", locale = "he", widthDp = 320)
@Composable
private fun ProposalHebrewDarkLargePreview() = PreviewSurface(
    dark = true, rtl = true, fontScale = 2f,
) { ProposalPreviewStates() }

@Preview(name = "Proposals · English · 200%", group = "Proposal", locale = "en", widthDp = 320)
@Composable
private fun ProposalEnglishLargePreview() = PreviewSurface(fontScale = 2f) {
    ProposalPreviewStates()
}

@Preview(name = "Proposals · wide", group = "Proposal", widthDp = 720)
@Composable
private fun ProposalWidePreview() = PreviewSurface { ProposalPreviewStates() }

@Composable
private fun PreviewSurface(
    dark: Boolean = false,
    rtl: Boolean = false,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
        LocalDensity provides Density(density.density, fontScale),
    ) {
        NotiMateTheme(darkTheme = dark) {
            Surface {
                Column(
                    modifier = Modifier.safeDrawingPadding().widthIn(max = 720.dp).fillMaxWidth()
                        .verticalScroll(rememberScrollState()).padding(NotiMateSpacing.space16),
                    verticalArrangement = Arrangement.spacedBy(NotiMateSpacing.space24),
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun ProcessingPreviewStates() {
    val states = listOf(
        ProcessingStatusUiState.Active(stringResource(Res.string.preview_profile)),
        ProcessingStatusUiState.Paused,
        ProcessingStatusUiState.PermissionRequired,
        ProcessingStatusUiState.Degraded(stringResource(Res.string.preview_degraded)),
        ProcessingStatusUiState.Unavailable(
            stringResource(Res.string.preview_unavailable), ProcessingRecovery.TRY_AGAIN,
        ),
        ProcessingStatusUiState.Unavailable(
            stringResource(Res.string.preview_deferred), ProcessingRecovery.REVIEW_LATER,
        ),
    )
    states.forEach { ProcessingStatusBanner(it, onAction = {}, modifier = Modifier.fillMaxWidth()) }
}

@Composable
private fun ProposalPreviewStates() {
    ProposalCardStatus.entries.forEach { status ->
        ProposalCard(
            state = ProposalCardUiState(
                title = stringResource(Res.string.preview_title),
                summary = stringResource(Res.string.preview_summary),
                sourceName = stringResource(Res.string.preview_source),
                profileName = stringResource(Res.string.preview_profile),
                automationName = stringResource(Res.string.preview_automation),
                status = status,
                explanation = stringResource(when (status) {
                    ProposalCardStatus.NEEDS_REVIEW -> Res.string.preview_needs_review
                    ProposalCardStatus.READY -> Res.string.preview_ready
                    ProposalCardStatus.DEFERRED -> Res.string.preview_deferred
                    ProposalCardStatus.FAILED -> Res.string.preview_failed
                    ProposalCardStatus.EXPIRED -> Res.string.preview_expired
                }),
            ),
            onAction = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
