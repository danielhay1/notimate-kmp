package com.hayduck.notemate.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Product meaning expressed as container/content pairs, always accompanied by visible text. */
@Immutable
data class NotiMateSemanticColors(
    val success: Color,
    val onSuccess: Color,
    val review: Color,
    val onReview: Color,
    val privacy: Color,
    val onPrivacy: Color,
    val paused: Color,
    val onPaused: Color,
    val aiAvailable: Color,
    val onAiAvailable: Color,
    val degraded: Color,
    val onDegraded: Color,
    val failure: Color,
    val onFailure: Color,
)
