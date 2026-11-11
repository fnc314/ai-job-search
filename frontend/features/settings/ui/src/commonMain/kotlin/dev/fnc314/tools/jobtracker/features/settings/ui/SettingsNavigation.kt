package dev.fnc314.tools.jobtracker.features.settings.ui

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey as PlatformNavKey
import dev.fnc314.tools.jobtracker.app.core.LocalAppNavigator
import dev.fnc314.tools.jobtracker.core.navigation.FeatureEntryRegistry
import dev.fnc314.tools.jobtracker.core.navigation.NavKey
import fnc314jobsearchtracker.features_settings_ui.generated.resources.Res
import fnc314jobsearchtracker.features_settings_ui.generated.resources.settings_route_title
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.jetbrains.compose.resources.StringResource

/**
 * Navigation destination route for the application settings screen.
 */
@Serializable
public data object JobApplicationSettingsRoute : NavKey {
    @Transient
    override val titleRes: StringResource = Res.string.settings_route_title
}

internal class SettingsEntryRegistry : FeatureEntryRegistry {
    override fun register(scope: EntryProviderScope<PlatformNavKey>) {
        scope.entry<JobApplicationSettingsRoute> {
            val navigator = LocalAppNavigator.current
            SettingsScreen(
                onNavigateBack = navigator::popBackStack,
                onSignOut = navigator::logout,
            )
        }
    }
}
