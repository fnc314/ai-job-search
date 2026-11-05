package dev.fnc314.tools.jobtracker.features.trackeredits.ui

import androidx.navigation.compose.composable
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.navigation.FeatureNavigationHook
import dev.fnc314.tools.jobtracker.core.navigation.NavKeyRoute
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.Res
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.edits_route_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource

@Serializable
public data object EditsRoute : NavKeyRoute {
    @Transient
    override val titleRes: StringResource = Res.string.edits_route_title
}

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
