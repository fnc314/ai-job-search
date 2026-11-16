package dev.fnc314.aijobtracker.frontend.core.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey as PlatformNavKey

/**
 * Interface allowing feature modules to register navigation entries without receiving
 * callback parameters. Implementations use `org.koin.compose.koinInject` to obtain
 * an [AppNavigator] for navigation actions.
 */
public fun interface FeatureEntryRegistry {
    /**
     * Registers feature navigation entries into the provided [scope].
     *
     * @param scope Navigation entry provider scope.
     */
    public operator fun invoke(scope: EntryProviderScope<PlatformNavKey>)
}
