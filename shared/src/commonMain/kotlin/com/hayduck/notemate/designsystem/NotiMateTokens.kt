package com.hayduck.notemate.designsystem

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object NotiMateSpacing {
    val space4 = 4.dp
    val space8 = 8.dp
    val space12 = 12.dp
    val space16 = 16.dp
    val space24 = 24.dp
    val space32 = 32.dp
    val space48 = 48.dp
}

object NotiMateShapes {
    val control = RoundedCornerShape(8.dp)
    val compactPanel = RoundedCornerShape(12.dp)
    val card = RoundedCornerShape(16.dp)
    val panel = RoundedCornerShape(24.dp)
    val pill = CircleShape

    internal val material = Shapes(
        extraSmall = control,
        small = compactPanel,
        medium = card,
        large = panel,
        extraLarge = panel,
    )
}
