package dev.fnc314.tools.jobtracker.core.designsystem

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthSizeClass {
    Compact,
    Medium,
    Expanded;

    companion object {
        fun fromWidth(width: Dp): WindowWidthSizeClass = when {
            width < 600.dp -> Compact
            width < 840.dp -> Medium
            else -> Expanded
        }
    }
}

enum class WindowHeightSizeClass {
    Compact,
    Medium,
    Expanded;

    companion object {
        fun fromHeight(height: Dp): WindowHeightSizeClass = when {
            height < 480.dp -> Compact
            height < 900.dp -> Medium
            else -> Expanded
        }
    }
}

data class WindowSizeClass(
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass,
) {
    val isCompact: Boolean get() = widthSizeClass == WindowWidthSizeClass.Compact
    val isMedium: Boolean get() = widthSizeClass == WindowWidthSizeClass.Medium
    val isExpanded: Boolean get() = widthSizeClass == WindowWidthSizeClass.Expanded

    companion object {
        fun calculate(width: Dp, height: Dp): WindowSizeClass = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.fromWidth(width),
            heightSizeClass = WindowHeightSizeClass.fromHeight(height),
        )
    }
}

val LocalWindowSizeClass = compositionLocalOf {
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
fun ProvideWindowSizeClass(
    modifier: Modifier = Modifier,
    content: @Composable (WindowSizeClass) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val windowSizeClass = WindowSizeClass.calculate(maxWidth, maxHeight)
        CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
            content(windowSizeClass)
        }
    }
}
