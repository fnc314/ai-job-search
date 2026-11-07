package dev.fnc314.tools.jobtracker.features.settings.ui

import androidx.navigation.compose.composable
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.navigation.FeatureNavigationHook
import dev.fnc314.tools.jobtracker.core.navigation.NavKey
import fnc314jobsearchtracker.features_settings_ui.generated.resources.Res
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_route_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource

/**
 * Navigation destination route for the application settings screen.
 */
@Serializable
public data object SettingsRoute : NavKey {
    @Transient
    override val titleRes: StringResource = Res.string.settings_route_title
}

/**
 * Creates the [FeatureNavigationHook] for the settings destination.
 *
 * @param onNavigateBack Callback invoked when the user exits the settings screen.
 * @param onSignOut Callback invoked when the user signs out.
 * @return Configured [FeatureNavigationHook] ready for graph registration.
 */
public fun settingsNavHook(
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
): FeatureNavigationHook = FeatureNavigationHook { navGraphBuilder, _ ->
    navGraphBuilder.composable<SettingsRoute> {
        SettingsScreen(
            onNavigateBack = onNavigateBack,
            onSignOut = onSignOut,
        )
    }
}

/**
 * Creates the [FeatureNavigationHook] for the settings destination with explicit settings listeners.
 *
 * @param settings Current [AppSettings] values.
 * @param onSettingsChange Callback receiving updated settings instances.
 * @param onNavigateBack Callback invoked when exiting the settings screen.
 * @param onSignOut Callback invoked when the user signs out.
 * @return Configured [FeatureNavigationHook] ready for graph registration.
 */
public fun settingsNavHook(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onNavigateBack: () -> Unit,
    onSignOut: () -> Unit,
): FeatureNavigationHook = settingsNavHook(
    onNavigateBack = onNavigateBack,
    onSignOut = onSignOut,
)
