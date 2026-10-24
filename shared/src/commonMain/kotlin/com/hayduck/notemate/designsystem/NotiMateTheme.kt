package com.hayduck.notemate.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val lightColors = lightColorScheme(
    primary = Color(0xFF315F55), onPrimary = Color.White,
    primaryContainer = Color(0xFFB5DACE), onPrimaryContainer = Color(0xFF0B211C),
    secondary = Color(0xFF4E635D), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1E8E0), onSecondaryContainer = Color(0xFF0B201B),
    tertiary = Color(0xFF5B5F7A), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE1E2FF), onTertiaryContainer = Color(0xFF171A32),
    error = Color(0xFFB3261E), onError = Color.White,
    errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF8FAF7), onBackground = Color(0xFF191C1A),
    surface = Color(0xFFF8FAF7), onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDDE5E0), onSurfaceVariant = Color(0xFF414945),
    outline = Color(0xFF717974), outlineVariant = Color(0xFFC1C9C4),
    inverseSurface = Color(0xFF191C1A), inverseOnSurface = Color(0xFFF8FAF7),
    inversePrimary = Color(0xFF9FCFC0), surfaceTint = Color(0xFF315F55),
    surfaceBright = Color(0xFFF8FAF7), surfaceDim = Color(0xFFDDE5E0),
    surfaceContainerLowest = Color(0xFFF8FAF7), surfaceContainerLow = Color(0xFFF8FAF7),
    surfaceContainer = Color(0xFFF8FAF7), surfaceContainerHigh = Color(0xFFDDE5E0),
    surfaceContainerHighest = Color(0xFFDDE5E0),
)

private val darkColors = darkColorScheme(
    primary = Color(0xFF9FCFC0), onPrimary = Color(0xFF00382E),
    primaryContainer = Color(0xFF164B41), onPrimaryContainer = Color(0xFFBBECE0),
    secondary = Color(0xFFB5CCC4), onSecondary = Color(0xFF21332E),
    secondaryContainer = Color(0xFF374B45), onSecondaryContainer = Color(0xFFD1E8E0),
    tertiary = Color(0xFFC2C5E5), onTertiary = Color(0xFF2C3049),
    tertiaryContainer = Color(0xFF424760), onTertiaryContainer = Color(0xFFE1E2FF),
    error = Color(0xFFF2B8B5), onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18), onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF101412), onBackground = Color(0xFFE0E3DF),
    surface = Color(0xFF101412), onSurface = Color(0xFFE0E3DF),
    surfaceVariant = Color(0xFF414945), onSurfaceVariant = Color(0xFFC1C9C4),
    outline = Color(0xFF8B938E), outlineVariant = Color(0xFF414945),
    inverseSurface = Color(0xFFE0E3DF), inverseOnSurface = Color(0xFF101412),
    inversePrimary = Color(0xFF315F55), surfaceTint = Color(0xFF9FCFC0),
    surfaceBright = Color(0xFF414945), surfaceDim = Color(0xFF101412),
    surfaceContainerLowest = Color(0xFF101412), surfaceContainerLow = Color(0xFF101412),
    surfaceContainer = Color(0xFF101412), surfaceContainerHigh = Color(0xFF414945),
    surfaceContainerHighest = Color(0xFF414945),
)

private val lightSemanticColors = NotiMateSemanticColors(
    success = Color(0xFFD5EBDD), onSuccess = Color(0xFF123722),
    review = Color(0xFFF6E9BE), onReview = Color(0xFF332B00),
    privacy = lightColors.secondaryContainer, onPrivacy = lightColors.onSecondaryContainer,
    paused = Color(0xFFE8E1D5), onPaused = Color(0xFF322E28),
    aiAvailable = lightColors.tertiaryContainer, onAiAvailable = lightColors.onTertiaryContainer,
    degraded = lightColors.surfaceVariant, onDegraded = lightColors.onSurfaceVariant,
    failure = lightColors.errorContainer, onFailure = lightColors.onErrorContainer,
)

private val darkSemanticColors = NotiMateSemanticColors(
    success = Color(0xFF244A34), onSuccess = Color(0xFFB8F0CA),
    review = Color(0xFF4C4300), onReview = Color(0xFFF6E9BE),
    privacy = darkColors.secondaryContainer, onPrivacy = darkColors.onSecondaryContainer,
    paused = Color(0xFF49443C), onPaused = Color(0xFFE8E1D5),
    aiAvailable = darkColors.tertiaryContainer, onAiAvailable = darkColors.onTertiaryContainer,
    degraded = darkColors.surfaceVariant, onDegraded = darkColors.onSurfaceVariant,
    failure = darkColors.errorContainer, onFailure = darkColors.onErrorContainer,
)

private val localSemanticColors = staticCompositionLocalOf { lightSemanticColors }

private fun typeStyle(size: Int, height: Int, weight: FontWeight) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = height.sp,
)

private val typography = Typography(
    headlineSmall = typeStyle(24, 32, FontWeight.Medium),
    titleLarge = typeStyle(22, 28, FontWeight.Medium),
    titleMedium = typeStyle(16, 24, FontWeight.Medium),
    titleSmall = typeStyle(14, 20, FontWeight.Medium),
    bodyLarge = typeStyle(16, 24, FontWeight.Normal),
    bodyMedium = typeStyle(14, 20, FontWeight.Normal),
    labelLarge = typeStyle(14, 20, FontWeight.Medium),
    bodySmall = typeStyle(12, 16, FontWeight.Normal),
)

/** Shared palette with a system-theme default and an explicit preview/preference override. */
@Composable
fun NotiMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        localSemanticColors provides if (darkTheme) darkSemanticColors else lightSemanticColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkColors else lightColors,
            typography = typography,
            shapes = NotiMateShapes.material,
            content = content,
        )
    }
}

object NotiMateTheme {
    val semanticColors: NotiMateSemanticColors
        @Composable get() = localSemanticColors.current
}
