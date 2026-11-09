package dev.fnc314.tools.jobtracker.core.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey as PlatformNavKey

/**
 * Functional interface allowing feature modules to register destinations in an [EntryProviderScope].
 */
public fun interface FeatureNavigationHook {
    public operator fun invoke(
        scope: EntryProviderScope<PlatformNavKey>,
        navigator: Navigator,
    )
}
