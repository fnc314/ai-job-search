package dev.fnc314.tools.jobtracker.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 4dp/8dp consistent layout grid spacing and padding tokens.
 *
 * @property none Zero spacing (`0.dp`).
 * @property hairline Hairline divider thickness (`1.dp`).
 * @property micro Micro gap (`2.dp`).
 * @property compact Compact inset (`3.dp`).
 * @property extraSmall Extra small padding (`4.dp`).
 * @property subSmall Sub-small padding (`6.dp`).
 * @property small Small padding (`8.dp`).
 * @property subMedium Sub-medium padding (`12.dp`).
 * @property medium Standard medium container padding (`16.dp`).
 * @property large Large section padding (`24.dp`).
 * @property extraLarge Extra large card spacing (`32.dp`).
 * @property extraExtraLarge Header clearance spacing (`48.dp`).
 * @property huge Page section boundary spacing (`64.dp`).
 * @property fabBottomPadding Clearance padding ensuring lists scroll clear of floating action buttons (`88.dp`).
 */
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

/**
 * CompositionLocal providing active [Spacing] values down the hierarchy.
 */
public val LocalSpacing: androidx.compose.runtime.ProvidableCompositionLocal<Spacing> = staticCompositionLocalOf { Spacing() }

/**
 * Accessor resolving the ambient [Spacing] from [MaterialTheme].
 */
public val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = LocalSpacing.current
