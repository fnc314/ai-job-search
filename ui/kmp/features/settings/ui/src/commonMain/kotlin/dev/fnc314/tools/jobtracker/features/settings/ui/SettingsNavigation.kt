package dev.fnc314.tools.jobtracker.features.settings.ui

import androidx.navigation.compose.composable
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.navigation.FeatureNavigationHook
import dev.fnc314.tools.jobtracker.core.navigation.NavKeyRoute
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
public data object SettingsRoute : NavKeyRoute

public fun settingsNavHook(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onNavigateBack: () -> Unit,
): FeatureNavigationHook = FeatureNavigationHook { navGraphBuilder, _ ->
    navGraphBuilder.composable<SettingsRoute> {
        val signInViewModel = koinViewModel<SignInViewModel>()
        SettingsScreen(
            settings = settings,
            onSettingsChange = onSettingsChange,
            onNavigateBack = onNavigateBack,
            signInViewModel = signInViewModel,
        )
    }
}
