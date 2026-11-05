package dev.fnc314.tools.jobtracker.core.designsystem

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

public enum class WindowWidthSizeClass {
    Compact,
    Medium,
    Expanded;

    public companion object {
        public fun fromWidth(width: Dp): WindowWidthSizeClass = when {
            width < 600.dp -> Compact
            width < 840.dp -> Medium
            else -> Expanded
        }
    }
}

public enum class WindowHeightSizeClass {
    Compact,
    Medium,
    Expanded;

    public companion object {
        public fun fromHeight(height: Dp): WindowHeightSizeClass = when {
            height < 480.dp -> Compact
            height < 900.dp -> Medium
            else -> Expanded
        }
    }
}

public data class WindowSizeClass(
    public val widthSizeClass: WindowWidthSizeClass,
    public val heightSizeClass: WindowHeightSizeClass,
) {
    public val isCompact: Boolean get() = widthSizeClass == WindowWidthSizeClass.Compact
    public val isMedium: Boolean get() = widthSizeClass == WindowWidthSizeClass.Medium
    public val isExpanded: Boolean get() = widthSizeClass == WindowWidthSizeClass.Expanded

    public companion object {
        public fun calculate(width: Dp, height: Dp): WindowSizeClass = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.fromWidth(width),
            heightSizeClass = WindowHeightSizeClass.fromHeight(height),
        )
    }
}

public val LocalWindowSizeClass: androidx.compose.runtime.ProvidableCompositionLocal<WindowSizeClass> = compositionLocalOf {
    WindowSizeClass(
        widthSizeClass = WindowWidthSizeClass.Compact,
        heightSizeClass = WindowHeightSizeClass.Medium,
    )
}

/**
 * Adaptive container that calculates current [WindowSizeClass] based on available
 * constraints and provides it down the composition tree via [LocalWindowSizeClass].
 */
@Composable
public fun ProvideWindowSizeClass(
    modifier: Modifier = Modifier,
    content: @Composable (WindowSizeClass) -> Unit,
): Unit {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val windowSizeClass = WindowSizeClass.calculate(maxWidth, maxHeight)
        CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
            content(windowSizeClass)
        }
    }
}
