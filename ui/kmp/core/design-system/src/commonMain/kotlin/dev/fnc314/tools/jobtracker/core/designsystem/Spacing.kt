package dev.fnc314.tools.jobtracker.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

public data class Spacing(
    public val none: Dp = 0.dp,
    public val hairline: Dp = 1.dp,
    public val micro: Dp = 2.dp,
    public val compact: Dp = 3.dp,
    public val extraSmall: Dp = 4.dp,
    public val subSmall: Dp = 6.dp,
    public val small: Dp = 8.dp,
    public val subMedium: Dp = 12.dp,
    public val medium: Dp = 16.dp,
    public val large: Dp = 24.dp,
    public val extraLarge: Dp = 32.dp,
    public val extraExtraLarge: Dp = 48.dp,
    public val huge: Dp = 64.dp,
    public val fabBottomPadding: Dp = 88.dp,
)

public val LocalSpacing: androidx.compose.runtime.ProvidableCompositionLocal<Spacing> = staticCompositionLocalOf { Spacing() }

public val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
