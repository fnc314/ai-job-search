package dev.fnc314.tools.jobtracker.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standardized icon dimensions across navigation bars, list rows, badges, and avatars.
 *
 * @property extraSmall Tiny badge or counter icon (`12.dp`).
 * @property small Small indicator icon (`16.dp`).
 * @property medium Medium chip icon (`18.dp`).
 * @property standard Action bar and list item icon (`20.dp`).
 * @property default Standard interactive icon (`24.dp`).
 * @property large Prominent section icon (`32.dp`).
 * @property extraLarge Hero icon (`48.dp`).
 * @property avatar Profile avatar dimension (`72.dp`).
 */
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

/**
 * Structural sizing tokens for complex widgets, responsive forms, and drag-and-drop cards.
 *
 * @property hairline Hairline divider line (`1.dp`).
 * @property timelineLineWidth Vertical connecting line width for timeline events (`2.dp`).
 * @property badgePaddingHorizontal Horizontal padding for status and fit badges (`8.dp`).
 * @property badgePaddingVertical Vertical padding for status and fit badges (`3.dp`).
 * @property iconButtonSize Standard icon button dimension (`36.dp`).
 * @property actionTargetSize Minimum touch target size for interactive elements (`44.dp`).
 * @property timelineNodeSize Node circle size on event timelines (`32.dp`).
 * @property elevationDraggingTonal Tonal elevation applied during card drag operations (`6.dp`).
 * @property elevationDraggingShadow Shadow elevation applied during card drag operations (`8.dp`).
 * @property elevationDraggingCard Card elevation applied during drag operations (`12.dp`).
 * @property listCardAdaptiveMin Minimum adaptive width for multi-column grid cards (`360.dp`).
 * @property formMaxWidth Maximum centered width for data entry forms (`760.dp`).
 * @property detailMaxWidth Maximum centered width for detail view layouts (`1200.dp`).
 * @property formBottomClearance Bottom scrolling clearance for keyboard and action buttons (`280.dp`).
 * @property companyLogoSmall Compact inline company logo dimension (`48.dp`).
 * @property companyLogoMedium Standard list card company logo dimension (`64.dp`).
 * @property companyLogoLarge Prominent wide card and overview company logo dimension (`72.dp`).
 * @property cardElevationDefault Default flat card elevation (`0.dp`).
 * @property cardElevationLow Subtle low card elevation (`1.dp`).
 * @property cardElevationMedium Medium tonal card elevation (`3.dp`).
 * @property cardElevationHigh High highlighted card elevation (`6.dp`).
 * @property chevronTouchWidth Touch target width for expandable card chevron indicator (`40.dp`).
 */
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
    public val companyLogoSmall: Dp = 48.dp,
    public val companyLogoMedium: Dp = 64.dp,
    public val companyLogoLarge: Dp = 72.dp,
    public val cardElevationDefault: Dp = 0.dp,
    public val cardElevationLow: Dp = 1.dp,
    public val cardElevationMedium: Dp = 3.dp,
    public val cardElevationHigh: Dp = 6.dp,
    public val chevronTouchWidth: Dp = 40.dp,
)

/** CompositionLocal providing active [IconSizes] values down the hierarchy. */
public val LocalIconSizes: androidx.compose.runtime.ProvidableCompositionLocal<IconSizes> =
    staticCompositionLocalOf { IconSizes() }

/** CompositionLocal providing active [ComponentSizes] values down the hierarchy. */
public val LocalComponentSizes: androidx.compose.runtime.ProvidableCompositionLocal<ComponentSizes> =
    staticCompositionLocalOf { ComponentSizes() }

/** Resolves active [IconSizes] from ambient [MaterialTheme]. */
public val MaterialTheme.iconSizes: IconSizes
    @Composable
    @ReadOnlyComposable
    get() = LocalIconSizes.current

/** Alias resolving active [IconSizes] from ambient [MaterialTheme]. */
public val MaterialTheme.iconSize: IconSizes
    @Composable
    @ReadOnlyComposable
    get() = LocalIconSizes.current

/** Resolves active [ComponentSizes] from ambient [MaterialTheme]. */
public val MaterialTheme.componentSizes: ComponentSizes
    @Composable
    @ReadOnlyComposable
    get() = LocalComponentSizes.current
