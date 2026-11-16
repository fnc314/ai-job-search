package dev.fnc314.aijobtracker.frontend.core.navigation

import androidx.navigation3.runtime.NavKey as AndroidxNavKey
import org.jetbrains.compose.resources.StringResource

/**
 * App-wide route contract extending Navigation 3 NavKey for destination routes.
 *
 * Each route declares its toolbar icon treatments via [topBarActions] so the app-level
 * `AppScaffold` can render the correct navigation icon, action buttons, and custom action
 * slots based solely on the top of the back stack.
 *
 * @property titleRes String resource for the top-bar title.
 * @property topBarActions Toolbar icon treatments requested by this destination.
 */
public interface JobApplicationsTrackerNavKey : AndroidxNavKey {
    /** Localized title string resource displayed in the top bar. */
    public val titleRes: StringResource

    /** Top bar icon and action button configuration requested by this destination. */
    public val topBarActions: TopBarActions get() = TopBarActions.None
}
