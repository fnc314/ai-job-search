package dev.fnc314.tools.jobtracker.core.designsystem

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsive window width classifications following Material 3 breakpoints.
 */
public enum class WindowWidthSizeClass {
    /** Compact width (< 600dp), typical for portrait phones. */
    Compact,

    /** Medium width (600dp .. 840dp), typical for small tablets and foldables. */
    Medium,

    /** Expanded width (> 840dp), typical for large tablets, desktop monitors, and web. */
    Expanded;

    public companion object {
        /**
         * Resolves the [WindowWidthSizeClass] given a viewport [width].
         *
         * @param width The available width in [Dp].
         * @return The matched [WindowWidthSizeClass].
         */
        public fun fromWidth(width: Dp): WindowWidthSizeClass = when {
            width < 600.dp -> Compact
            width < 840.dp -> Medium
            else -> Expanded
        }
    }
}

/**
 * Responsive window height classifications following Material 3 breakpoints.
 */
public enum class WindowHeightSizeClass {
    /** Compact height (< 480dp), typical for landscape phones. */
    Compact,

    /** Medium height (480dp .. 900dp), typical for portrait phones and tablets. */
    Medium,

    /** Expanded height (> 900dp), typical for large desktop windows. */
    Expanded;

    public companion object {
        /**
         * Resolves the [WindowHeightSizeClass] given a viewport [height].
         *
         * @param height The available height in [Dp].
         * @return The matched [WindowHeightSizeClass].
         */
        public fun fromHeight(height: Dp): WindowHeightSizeClass = when {
            height < 480.dp -> Compact
            height < 900.dp -> Medium
            else -> Expanded
        }
    }
}

/**
 * Multiplatform window size class encapsulating width and height bucket classifications.
 *
 * @property widthSizeClass Calculated horizontal size classification [WindowWidthSizeClass].
 * @property heightSizeClass Calculated vertical size classification [WindowHeightSizeClass].
 */
public data class WindowSizeClass(
    public val widthSizeClass: WindowWidthSizeClass,
    public val heightSizeClass: WindowHeightSizeClass,
) {
    /** Convenience flag indicating whether width is [WindowWidthSizeClass.Compact]. */
    public val isCompact: Boolean get() = widthSizeClass == WindowWidthSizeClass.Compact

    /** Convenience flag indicating whether width is [WindowWidthSizeClass.Medium]. */
    public val isMedium: Boolean get() = widthSizeClass == WindowWidthSizeClass.Medium

    /** Convenience flag indicating whether width is [WindowWidthSizeClass.Expanded]. */
    public val isExpanded: Boolean get() = widthSizeClass == WindowWidthSizeClass.Expanded

    public companion object {
        /**
         * Computes a [WindowSizeClass] from viewport width and height dimensions.
         *
         * @param width Available width in [Dp].
         * @param height Available height in [Dp].
         * @return Calculated [WindowSizeClass].
         */
        public fun calculate(width: Dp, height: Dp): WindowSizeClass = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.fromWidth(width),
            heightSizeClass = WindowHeightSizeClass.fromHeight(height),
        )
    }
}

/**
 * CompositionLocal providing active [WindowSizeClass] information down the composable hierarchy.
 */
public val LocalWindowSizeClass: androidx.compose.runtime.ProvidableCompositionLocal<WindowSizeClass> = compositionLocalOf {
    WindowSizeClass(WindowWidthSizeClass.Compact, WindowHeightSizeClass.Medium)
}

/**
 * Adaptive container calculating current window size classes and propagating [LocalWindowSizeClass] to [content].
 *
 * @param modifier Layout modifier applied to the root container.
 * @param content Child composable content receiving the ambient window size class.
 */
@Composable
public fun ProvideWindowSizeClass(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
): Unit {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val windowSizeClass = WindowSizeClass.calculate(maxWidth, maxHeight)
        CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
            content()
        }
    }
}
