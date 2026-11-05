package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import androidx.navigation.compose.composable
import dev.fnc314.tools.jobtracker.core.navigation.FeatureNavigationHook
import dev.fnc314.tools.jobtracker.core.navigation.NavKeyRoute
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.Res
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_app_route_title
import fnc314jobsearchtracker.features_add_job_application_ui.generated.resources.add_lead_route_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource

/**
 * Navigation destination route for creating a formal job application entry.
 */
@Serializable
public data object AddApplicationRoute : NavKeyRoute {
    @Transient
    override val titleRes: StringResource = Res.string.add_app_route_title
}

/**
 * Navigation destination route for recording an early-stage prospective lead.
 */
@Serializable
public data object AddLeadRoute : NavKeyRoute {
    @Transient
    override val titleRes: StringResource = Res.string.add_lead_route_title
}

/**
 * Creates the [FeatureNavigationHook] for the add application and add lead destinations.
 *
 * @param onNavigateBack Callback invoked when the user exits or finishes the creation flow.
 * @return Configured [FeatureNavigationHook] ready for graph registration.
 */
public fun addJobApplicationNavHook(
    onNavigateBack: () -> Unit,
): FeatureNavigationHook = FeatureNavigationHook { navGraphBuilder, _ ->
    navGraphBuilder.composable<AddApplicationRoute> {
        AddJobApplicationScreen(
            onNavigateBack = onNavigateBack,
        )
    }
    navGraphBuilder.composable<AddLeadRoute> {
        AddLeadScreen(
            onNavigateBack = onNavigateBack,
        )
    }
}
