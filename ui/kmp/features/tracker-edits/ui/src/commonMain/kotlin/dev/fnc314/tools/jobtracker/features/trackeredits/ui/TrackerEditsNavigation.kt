package dev.fnc314.tools.jobtracker.features.trackeredits.ui

import androidx.navigation.compose.composable
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.navigation.FeatureNavigationHook
import dev.fnc314.tools.jobtracker.core.navigation.NavKeyRoute
import kotlinx.serialization.Serializable

@Serializable
public data object EditsRoute : NavKeyRoute

public fun trackerEditsNavHook(
    onNavigateBack: () -> Unit,
    onEditClick: (String) -> Unit = {},
    settings: AppSettings = AppSettings(),
    onSettingsChange: (AppSettings) -> Unit = {},
): FeatureNavigationHook = FeatureNavigationHook { navGraphBuilder, _ ->
    navGraphBuilder.composable<EditsRoute> {
        TrackerEditsScreen(
            onNavigateBack = onNavigateBack,
            onEditClick = onEditClick,
            settings = settings,
            onSettingsChange = onSettingsChange,
        )
    }
}
