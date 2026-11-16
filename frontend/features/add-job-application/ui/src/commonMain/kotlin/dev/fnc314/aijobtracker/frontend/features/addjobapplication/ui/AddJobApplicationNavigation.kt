package dev.fnc314.aijobtracker.frontend.features.addjobapplication.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey as PlatformNavKey
import dev.fnc314.aijobtracker.frontend.core.navigation.LocalAppNavigator
import dev.fnc314.aijobtracker.frontend.core.navigation.FeatureEntryRegistry
import dev.fnc314.aijobtracker.frontend.core.navigation.JobApplicationsTrackerNavKey
import dev.fnc314.aijobtracker.frontend.core.navigation.TopBarActions
import dev.fnc314.aijobtracker.frontend.features_add_job_application_ui.generated.resources.Res
import dev.fnc314.aijobtracker.frontend.features_add_job_application_ui.generated.resources.add_app_route_title
import dev.fnc314.aijobtracker.frontend.features_add_job_application_ui.generated.resources.add_lead_route_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource

/**
 * Navigation destination route for creating a formal job application entry.
 */
@Serializable
public data object JobApplicationAddApplicationRoute : JobApplicationsTrackerNavKey {
     @Transient
     override val titleRes: StringResource = Res.string.add_app_route_title

     @Transient
     override val topBarActions: TopBarActions = TopBarActions.Detail
}

/**
 * Navigation destination route for recording an early-stage prospective lead.
 */
@Serializable
public data object JobApplicationAddLeadRoute : JobApplicationsTrackerNavKey {
    @Transient
    override val titleRes: StringResource = Res.string.add_lead_route_title

    @Transient
    override val topBarActions: TopBarActions = TopBarActions.Detail
}

/**
 * Navigation entry registry providing routes for adding job applications and leads.
 */
public class AddJobApplicationEntryRegistry : FeatureEntryRegistry {
    override operator fun invoke(scope: EntryProviderScope<PlatformNavKey>) {
        scope.entry<JobApplicationAddApplicationRoute> {
            val navigator = LocalAppNavigator.current
            AddJobApplicationScreen(
                onNavigateBack = navigator::popBackStack,
            )
        }
        scope.entry<JobApplicationAddLeadRoute> {
            val navigator = LocalAppNavigator.current
            AddLeadScreen(
                onNavigateBack = navigator::popBackStack,
            )
        }
    }
}
