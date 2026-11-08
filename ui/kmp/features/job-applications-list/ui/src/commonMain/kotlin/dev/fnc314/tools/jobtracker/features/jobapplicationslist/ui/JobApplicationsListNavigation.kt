package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui

import androidx.navigation.compose.composable
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.navigation.FeatureNavigationHook
import dev.fnc314.tools.jobtracker.core.navigation.NavKey
import fnc314jobsearchtracker.features_job_application_list_ui.generated.resources.Res
import fnc314jobsearchtracker.features_job_application_list_ui.generated.resources.list_route_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource

@Serializable
public data object ListRoute : NavKey {
    @Transient
    override val titleRes: StringResource = Res.string.list_route_title
}

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
