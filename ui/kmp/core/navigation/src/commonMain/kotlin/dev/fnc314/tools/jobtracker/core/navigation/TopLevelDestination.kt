package dev.fnc314.tools.jobtracker.core.navigation

import androidx.compose.ui.graphics.vector.ImageVector

public interface TopLevelDestination {
    public val route: NavKeyRoute
    public val label: String
    public val selectedIcon: ImageVector
    public val unselectedIcon: ImageVector
    public val contentDescription: String

    public companion object {
        public fun from(
            route: NavKeyRoute,
            label: String,
            selectedIcon: ImageVector,
            unselectedIcon: ImageVector,
            contentDescription: String,
        ): TopLevelDestination = TopLevelDestinationImpl(
            route = route,
            label = label,
            selectedIcon = selectedIcon,
            unselectedIcon = unselectedIcon,
            contentDescription = contentDescription,
        )
    }
}

internal data class TopLevelDestinationImpl(
    override val route: NavKeyRoute,
    override val label: String,
    override val selectedIcon: ImageVector,
    override val unselectedIcon: ImageVector,
    override val contentDescription: String,
) : TopLevelDestination
