package dev.fnc314.tools.jobtracker.features.addjobapplication.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey as PlatformNavKey
import dev.fnc314.tools.jobtracker.app.core.LocalAppNavigator
import dev.fnc314.tools.jobtracker.core.navigation.FeatureEntryRegistry
import dev.fnc314.tools.jobtracker.core.navigation.NavKey
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
public data object JobApplicationAddApplicationRoute : NavKey {
    @Transient
    override val titleRes: StringResource = Res.string.add_app_route_title
}

/**
 * Navigation destination route for recording an early-stage prospective lead.
 */
@Serializable
public data object JobApplicationAddLeadRoute : NavKey {
    @Transient
    override val titleRes: StringResource = Res.string.add_lead_route_title
}

internal class AddJobApplicationEntryRegistry : FeatureEntryRegistry {
    override fun register(scope: EntryProviderScope<PlatformNavKey>) {
        val navigator = LocalAppNavigator.current
        scope.entry<JobApplicationAddApplicationRoute> {
            AddJobApplicationScreen(
                onNavigateBack = navigator::popBackStack,
            )
        }
        scope.entry<JobApplicationAddLeadRoute> {
            AddLeadScreen(
                onNavigateBack = navigator::popBackStack,
            )
        }
    }
}
