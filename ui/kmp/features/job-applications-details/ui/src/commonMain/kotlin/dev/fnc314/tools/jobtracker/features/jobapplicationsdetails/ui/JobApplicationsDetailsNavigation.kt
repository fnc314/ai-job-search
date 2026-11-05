package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui

import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.navigation.FeatureNavigationHook
import dev.fnc314.tools.jobtracker.core.navigation.NavKeyRoute
import fnc314jobsearchtracker.features_job_applications_details_ui.generated.resources.Res
import fnc314jobsearchtracker.features_job_applications_details_ui.generated.resources.detail_route_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource

@Serializable
public data class DetailRoute(public val applicationId: String) : NavKeyRoute {
    @Transient
    override val titleRes: StringResource = Res.string.detail_route_title
}

public fun jobApplicationsDetailsNavHook(
    settings: AppSettings = AppSettings(),
    onNavigateBack: () -> Unit,
): FeatureNavigationHook = FeatureNavigationHook { navGraphBuilder, _ ->
    navGraphBuilder.composable<DetailRoute> { backStackEntry ->
        val route: DetailRoute = backStackEntry.toRoute()
        JobApplicationsDetailsScreen(
            applicationId = route.applicationId,
            onNavigateBack = onNavigateBack,
            settings = settings,
        )
    }
}
