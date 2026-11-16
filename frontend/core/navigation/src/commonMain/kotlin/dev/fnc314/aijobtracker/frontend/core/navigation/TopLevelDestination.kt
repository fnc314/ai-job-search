package dev.fnc314.aijobtracker.frontend.core.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource

/**
 * Root navigation bar or rail destination displayed in top-level scaffolding.
 */
public interface TopLevelDestination {
    /** Target navigation route key for this destination. */
    public val route: JobApplicationsTrackerNavKey

    /** Localized title string resource. */
    public val labelRes: StringResource

    /** Visual icon vector when this destination is active. */
    public val selectedIcon: ImageVector

    /** Visual icon vector when this destination is inactive. */
    public val unselectedIcon: ImageVector

    /** Localized content description string resource for accessibility. */
    public val contentDescriptionRes: StringResource

    /** Companion factory providing constructor-like instantiation for [TopLevelDestination]. */
    public companion object {
        /**
         * Factory function creating a [TopLevelDestination] instance.
         */
        public fun from(
            route: JobApplicationsTrackerNavKey,
            labelRes: StringResource,
            selectedIcon: ImageVector,
            unselectedIcon: ImageVector,
            contentDescriptionRes: StringResource,
        ): TopLevelDestination = TopLevelDestinationImpl(
            route = route,
            labelRes = labelRes,
            selectedIcon = selectedIcon,
            unselectedIcon = unselectedIcon,
            contentDescriptionRes = contentDescriptionRes,
        )
    }
}

private data class TopLevelDestinationImpl(
    override val route: JobApplicationsTrackerNavKey,
    override val labelRes: StringResource,
    override val selectedIcon: ImageVector,
    override val unselectedIcon: ImageVector,
    override val contentDescriptionRes: StringResource,
) : TopLevelDestination

