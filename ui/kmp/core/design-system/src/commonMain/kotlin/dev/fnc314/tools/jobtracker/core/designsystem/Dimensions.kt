package dev.fnc314.tools.jobtracker.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class IconSizes(
    val extraSmall: Dp = 12.dp,
    val small: Dp = 16.dp,
    val medium: Dp = 18.dp,
    val standard: Dp = 20.dp,
    val default: Dp = 24.dp,
    val large: Dp = 32.dp,
    val extraLarge: Dp = 48.dp,
    val avatar: Dp = 72.dp,
)

data class ComponentSizes(
    val hairline: Dp = 1.dp,
    val timelineLineWidth: Dp = 2.dp,
    val badgePaddingHorizontal: Dp = 8.dp,
    val badgePaddingVertical: Dp = 3.dp,
    val iconButtonSize: Dp = 36.dp,
    val actionTargetSize: Dp = 44.dp,
    val timelineNodeSize: Dp = 32.dp,
    val elevationDraggingTonal: Dp = 6.dp,
    val elevationDraggingShadow: Dp = 8.dp,
    val elevationDraggingCard: Dp = 12.dp,
    val listCardAdaptiveMin: Dp = 360.dp,
    val formMaxWidth: Dp = 760.dp,
    val detailMaxWidth: Dp = 1200.dp,
    val formBottomClearance: Dp = 280.dp,
)

val LocalIconSizes = staticCompositionLocalOf { IconSizes() }
val LocalComponentSizes = staticCompositionLocalOf { ComponentSizes() }

val MaterialTheme.iconSizes: IconSizes
    @Composable
    @ReadOnlyComposable
    get() = LocalIconSizes.current

val MaterialTheme.iconSize: IconSizes
    @Composable
    @ReadOnlyComposable
    get() = LocalIconSizes.current

val MaterialTheme.componentSizes: ComponentSizes
    @Composable
    @ReadOnlyComposable
    get() = LocalComponentSizes.current

val MaterialTheme.componentSize: ComponentSizes
    @Composable
    @ReadOnlyComposable
    get() = LocalComponentSizes.current
