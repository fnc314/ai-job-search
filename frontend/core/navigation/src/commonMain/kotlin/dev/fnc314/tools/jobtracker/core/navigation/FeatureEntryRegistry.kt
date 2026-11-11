package dev.fnc314.tools.jobtracker.core.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey as PlatformNavKey

/**
 * Interface allowing feature modules to register navigation entries without receiving
 * callback parameters. Implementations use [org.koin.compose.koinInject] to obtain
 * an [AppNavigator] for navigation actions.
 */
public interface FeatureEntryRegistry {
    public fun register(scope: EntryProviderScope<PlatformNavKey>)
}
