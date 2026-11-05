package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui

import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.navigation.FeatureNavigationHook
import dev.fnc314.tools.jobtracker.core.navigation.NavKeyRoute
import kotlinx.serialization.Serializable

@Serializable
public data class DetailRoute(public val applicationId: String) : NavKeyRoute

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
