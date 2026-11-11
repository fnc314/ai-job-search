package dev.fnc314.tools.jobtracker.features.trackeredits.ui

import androidx.compose.runtime.collectAsState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey as PlatformNavKey
import dev.fnc314.tools.jobtracker.app.core.LocalAppNavigator
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettingsRepository
import dev.fnc314.tools.jobtracker.core.navigation.ApplicationId
import dev.fnc314.tools.jobtracker.core.navigation.FeatureEntryRegistry
import dev.fnc314.tools.jobtracker.core.navigation.NavKey
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.Res
import fnc314jobsearchtracker.features_tracker_edits_ui.generated.resources.edits_route_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource
import org.koin.compose.koinInject

/**
 * Navigation destination route for the proposed edits review dashboard.
 */
@Serializable
public data object JobApplicationEditsRoute : NavKey {
  @Transient
  override val titleRes: StringResource = Res.string.edits_route_title
}

internal class TrackerEditsEntryRegistry : FeatureEntryRegistry {
  override fun register(scope: EntryProviderScope<PlatformNavKey>) {
    scope.entry<JobApplicationEditsRoute> {
      val navigator = LocalAppNavigator.current
      val settingsRepository = koinInject<AppSettingsRepository>()
      val settings by settingsRepository.settings.collectAsState()
      TrackerEditsScreen(
        onNavigateBack = navigator::popBackStack,
        onEditClick = { id ->
          navigator.navigateToApplicationDetails(ApplicationId(id))
        },
        settings = settings,
        onSettingsChange = { settingsRepository.updateSettings(it) },
      )
    }
  }
}
