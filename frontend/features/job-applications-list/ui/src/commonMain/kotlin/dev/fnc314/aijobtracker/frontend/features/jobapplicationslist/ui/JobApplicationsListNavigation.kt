package dev.fnc314.aijobtracker.frontend.features.jobapplicationslist.ui

import androidx.compose.runtime.collectAsState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey as PlatformNavKey
import dev.fnc314.aijobtracker.frontend.core.navigation.LocalAppNavigator
import dev.fnc314.aijobtracker.frontend.core.data.settings.AppSettingsRepository
import dev.fnc314.aijobtracker.frontend.core.navigation.ApplicationId
import dev.fnc314.aijobtracker.frontend.core.navigation.FeatureEntryRegistry
import dev.fnc314.aijobtracker.frontend.core.navigation.JobApplicationsTrackerNavKey
import dev.fnc314.aijobtracker.frontend.core.navigation.TopBarActions
import dev.fnc314.aijobtracker.frontend.core.navigation.TopLevelDestinationEnum
import dev.fnc314.aijobtracker.frontend.features.addjobapplication.ui.JobApplicationAddApplicationRoute
import dev.fnc314.aijobtracker.frontend.features.addjobapplication.ui.JobApplicationAddLeadRoute
import ai_job_tracker_frontend.features_job_applications_list_ui.generated.resources.Res
import ai_job_tracker_frontend.features_job_applications_list_ui.generated.resources.list_route_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.koinInject

/**
 * Navigation destination route for the job applications list directory.
 */
@Serializable
public data object JobApplicationListRoute : JobApplicationsTrackerNavKey {
    @Transient
    override val titleRes: StringResource = Res.string.list_route_title

    @Transient
    override val topBarActions: TopBarActions = TopBarActions.TopLevel
}

/**
 * Navigation entry registry providing routes for the job applications list feature.
 */
public class JobApplicationsEntryRegistry : FeatureEntryRegistry {
    override fun register(scope: EntryProviderScope<PlatformNavKey>) {
        scope.entry<JobApplicationListRoute> {
            val navigator = LocalAppNavigator.current
            val settingsRepository = koinInject<AppSettingsRepository>()
            val settings = settingsRepository.settings.collectAsState().value
            JobApplicationsListScreen(
                onApplicationClick = { id ->
                    navigator.navigateToApplicationDetails(ApplicationId(id))
                },
                onAddApplicationClick = {
                    navigator.navigate(JobApplicationAddApplicationRoute)
                },
                onAddLeadClick = {
                    navigator.navigate(JobApplicationAddLeadRoute)
                },
                onEditsClick = {
                    navigator.navigateTo(TopLevelDestinationEnum.Edits)
                },
                onSettingsClick = {
                    navigator.navigateToSettings()
                },
                settings = settings,
                onSettingsChange = { settingsRepository.updateSettings(it) },
            )
        }
    }
}
