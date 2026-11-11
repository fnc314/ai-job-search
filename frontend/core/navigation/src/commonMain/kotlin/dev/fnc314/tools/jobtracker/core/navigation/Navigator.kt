package dev.fnc314.tools.jobtracker.core.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey as PlatformNavKey

/**
 * Controller wrapping the Navigation 3 user-owned [NavBackStack].
 *
 * Provides operations to push, pop, and reset navigation states.
 *
 * @property backStack Active snapshot-state back stack of [PlatformNavKey] entries.
 */
public class Navigator(
    public val backStack: NavBackStack<PlatformNavKey>,
) {
    /** Pushes a new navigation key onto the back stack. */
    public fun navigate(key: JobApplicationsTrackerNavKey) {
        backStack.add(key)
    }

    /** Pops the top entry from the back stack if more than one entry exists. */
    public fun popBackStack(): Boolean {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
            return true
        }
        return false
    }

    /** Pops all entries except the root entry. */
    public fun popToRoot() {
        if (backStack.size > 1) {
            val root = backStack.first()
            backStack.clear()
            backStack.add(root)
        }
    }

    /** Replaces the entire back stack with a new root key. */
    public fun switchRoot(key: JobApplicationsTrackerNavKey) {
        backStack.clear()
        backStack.add(key)
    }
}
