package dev.fnc314.aijobtracker.frontend.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.ProvidableCompositionLocal

/**
 * CompositionLocal providing [AppNavigator] to all descendant composables.
 *
 * Feature screens read this value via [LocalAppNavigator.current] instead of receiving
 * navigation callbacks through composable parameters.
 */
public val LocalAppNavigator: ProvidableCompositionLocal<AppNavigator> =
    staticCompositionLocalOf { throw IllegalStateException("No AppNavigator provided in composition") }

/**
 * Provides [AppNavigator] to all descendant composables within [content].
 *
 * @param navigator The [AppNavigator] instance to provide.
 * @param content Content that can access the navigator via [LocalAppNavigator.current].
 */
@Composable
public fun ProvideAppNavigator(
    navigator: AppNavigator,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAppNavigator provides navigator, content = content)
}
