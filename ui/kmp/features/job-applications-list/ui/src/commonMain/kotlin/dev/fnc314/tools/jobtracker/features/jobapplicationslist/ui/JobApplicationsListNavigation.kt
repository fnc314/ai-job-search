package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui

import androidx.navigation.compose.composable
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.navigation.FeatureNavigationHook
import dev.fnc314.tools.jobtracker.core.navigation.NavKeyRoute
import kotlinx.serialization.Serializable

@Serializable
public data object ListRoute : NavKeyRoute

public fun jobApplicationsListNavHook(
    onApplicationClick: (String) -> Unit,
    onAddApplicationClick: () -> Unit = {},
    onAddLeadClick: () -> Unit = {},
    onEditsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    settings: AppSettings = AppSettings(),
    onSettingsChange: (AppSettings) -> Unit = {},
): FeatureNavigationHook = FeatureNavigationHook { navGraphBuilder, _ ->
    navGraphBuilder.composable<ListRoute> {
        JobApplicationsListScreen(
            onApplicationClick = onApplicationClick,
            onAddApplicationClick = onAddApplicationClick,
            onAddLeadClick = onAddLeadClick,
            onEditsClick = onEditsClick,
            onSettingsClick = onSettingsClick,
            settings = settings,
            onSettingsChange = onSettingsChange,
        )
    }
}
