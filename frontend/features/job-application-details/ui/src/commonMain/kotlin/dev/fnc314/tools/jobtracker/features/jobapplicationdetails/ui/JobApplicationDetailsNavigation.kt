package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui

import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.navigation.FeatureNavigationHook
import dev.fnc314.tools.jobtracker.core.navigation.NavKey
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.Res
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_route_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource

/**
 * Navigation destination route for viewing detailed information about a specific job application.
 *
 * @property applicationId Unique Firestore identifier of the job application.
 */
@Serializable
public data class DetailRoute(public val applicationId: String) : NavKey {
    @Transient
    override val titleRes: StringResource = Res.string.detail_route_title
}

/**
 * Creates the [FeatureNavigationHook] for the application details destination.
 *
 * Registers the [DetailRoute] composable within the navigation scope.
 *
 * @param settings Current [AppSettings] controlling visual and regional preferences.
 * @param onNavigateBack Callback invoked when the user requests returning to the prior screen.
 * @return Configured [FeatureNavigationHook] ready for scope registration.
 */
public fun jobApplicationDetailsNavHook(
    settings: AppSettings = AppSettings(),
    onNavigateBack: () -> Unit,
): FeatureNavigationHook =
    FeatureNavigationHook { scope, _ ->
        scope.entry<DetailRoute> { key ->
            JobApplicationDetailsScreen(
                applicationId = key.applicationId,
                onNavigateBack = onNavigateBack,
                settings = settings,
            )
        }
    }
