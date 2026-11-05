package dev.fnc314.tools.jobtracker.features.settings.ui

import androidx.navigation.compose.composable
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.navigation.FeatureNavigationHook
import dev.fnc314.tools.jobtracker.core.navigation.NavKeyRoute
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInViewModel
import fnc314jobsearchtracker.features_settings_ui.generated.resources.Res
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_route_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Navigation destination route for the application settings screen.
 */
@Serializable
public data object SettingsRoute : NavKeyRoute {
    @Transient
    override val titleRes: StringResource = Res.string.settings_route_title
}

/**
 * Creates the [FeatureNavigationHook] for the settings destination.
 *
 * @param onNavigateBack Callback invoked when the user exits the settings screen.
 * @return Configured [FeatureNavigationHook] ready for graph registration.
 */
public fun settingsNavHook(
    onNavigateBack: () -> Unit,
): FeatureNavigationHook = FeatureNavigationHook { navGraphBuilder, _ ->
    navGraphBuilder.composable<SettingsRoute> {
        val signInViewModel: SignInViewModel = koinViewModel()
        SettingsScreen(
            onNavigateBack = onNavigateBack,
            signInViewModel = signInViewModel,
        )
    }
}

/**
 * Creates the [FeatureNavigationHook] for the settings destination with explicit settings listeners.
 *
 * @param settings Current [AppSettings] values.
 * @param onSettingsChange Callback receiving updated settings instances.
 * @param onNavigateBack Callback invoked when exiting the settings screen.
 * @return Configured [FeatureNavigationHook] ready for graph registration.
 */
public fun settingsNavHook(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onNavigateBack: () -> Unit,
): FeatureNavigationHook = settingsNavHook(
    onNavigateBack = onNavigateBack,
)
