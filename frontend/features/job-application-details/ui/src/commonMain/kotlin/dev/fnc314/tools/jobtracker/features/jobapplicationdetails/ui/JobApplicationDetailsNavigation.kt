package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui

import androidx.compose.runtime.collectAsState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey as PlatformNavKey
import dev.fnc314.tools.jobtracker.app.core.LocalAppNavigator
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettingsRepository
import dev.fnc314.tools.jobtracker.core.navigation.FeatureEntryRegistry
import dev.fnc314.tools.jobtracker.core.navigation.NavKey
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.Res
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_route_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.koinInject

/**
 * Navigation destination route for viewing detailed information about a specific job application.
 *
 * @property applicationId Unique Firestore identifier of the job application (minimal input).
 */
@Serializable
public data class JobApplicationDetailRoute(
    public val applicationId: String,
) : NavKey {
    @Transient
    override val titleRes: StringResource = Res.string.detail_route_title
}

internal class JobApplicationDetailsEntryRegistry : FeatureEntryRegistry {
    override fun register(scope: EntryProviderScope<PlatformNavKey>) {
        scope.entry<JobApplicationDetailRoute> { key ->
            val navigator = LocalAppNavigator.current
            val settingsRepository = koinInject<AppSettingsRepository>()
            val settings by settingsRepository.settings.collectAsState()
            JobApplicationDetailsScreen(
                applicationId = key.applicationId,
                onNavigateBack = navigator::popBackStack,
                settings = settings,
            )
        }
    }
}
