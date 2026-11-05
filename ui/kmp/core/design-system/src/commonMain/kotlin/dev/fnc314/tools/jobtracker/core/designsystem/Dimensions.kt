package dev.fnc314.tools.jobtracker.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

public data class IconSizes(
    public val extraSmall: Dp = 12.dp,
    public val small: Dp = 16.dp,
    public val medium: Dp = 18.dp,
    public val standard: Dp = 20.dp,
    public val default: Dp = 24.dp,
    public val large: Dp = 32.dp,
    public val extraLarge: Dp = 48.dp,
    public val avatar: Dp = 72.dp,
)

public data class ComponentSizes(
    public val hairline: Dp = 1.dp,
    public val timelineLineWidth: Dp = 2.dp,
    public val badgePaddingHorizontal: Dp = 8.dp,
    public val badgePaddingVertical: Dp = 3.dp,
    public val iconButtonSize: Dp = 36.dp,
    public val actionTargetSize: Dp = 44.dp,
    public val timelineNodeSize: Dp = 32.dp,
    public val elevationDraggingTonal: Dp = 6.dp,
    public val elevationDraggingShadow: Dp = 8.dp,
    public val elevationDraggingCard: Dp = 12.dp,
    public val listCardAdaptiveMin: Dp = 360.dp,
    public val formMaxWidth: Dp = 760.dp,
    public val detailMaxWidth: Dp = 1200.dp,
    public val formBottomClearance: Dp = 280.dp,
)

public val LocalIconSizes: androidx.compose.runtime.ProvidableCompositionLocal<IconSizes> = staticCompositionLocalOf { IconSizes() }
public val LocalComponentSizes: androidx.compose.runtime.ProvidableCompositionLocal<ComponentSizes> = staticCompositionLocalOf { ComponentSizes() }

public val MaterialTheme.iconSizes: IconSizes
    @Composable
    @ReadOnlyComposable
    get() = LocalIconSizes.current

public val MaterialTheme.iconSize: IconSizes
    @Composable
    @ReadOnlyComposable
    get() = LocalIconSizes.current

public val MaterialTheme.componentSizes: ComponentSizes
    @Composable
    @ReadOnlyComposable
    get() = LocalComponentSizes.current

public val MaterialTheme.componentSize: ComponentSizes
    @Composable
    @ReadOnlyComposable
    get() = LocalComponentSizes.current
