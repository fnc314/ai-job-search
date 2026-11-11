package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui

import androidx.compose.runtime.collectAsState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey as PlatformNavKey
import dev.fnc314.tools.jobtracker.app.core.LocalAppNavigator
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettingsRepository
import dev.fnc314.tools.jobtracker.core.navigation.ApplicationId
import dev.fnc314.tools.jobtracker.core.navigation.FeatureEntryRegistry
import dev.fnc314.tools.jobtracker.core.navigation.NavKey
import dev.fnc314.tools.jobtracker.core.navigation.TopLevelDestinationEnum
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.JobApplicationAddApplicationRoute
import dev.fnc314.tools.jobtracker.features.addjobapplication.ui.JobApplicationAddLeadRoute
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.Res
import fnc314jobsearchtracker.features_job_applications_list_ui.generated.resources.list_route_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.koinInject

@Serializable
public data object JobApplicationListRoute : NavKey {
    @Transient
    override val titleRes: StringResource = Res.string.list_route_title
}

internal class JobApplicationsEntryRegistry : FeatureEntryRegistry {
    override fun register(scope: EntryProviderScope<PlatformNavKey>) {
        scope.entry<JobApplicationListRoute> {
            val navigator = LocalAppNavigator.current
            val settingsRepository = koinInject<AppSettingsRepository>()
            val settings by settingsRepository.settings.collectAsState()
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
