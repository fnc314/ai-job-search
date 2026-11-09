package dev.fnc314.tools.jobtracker.core.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource

public interface TopLevelDestination {
    public val route: NavKey
    public val labelRes: StringResource
    public val selectedIcon: ImageVector
    public val unselectedIcon: ImageVector
    public val contentDescriptionRes: StringResource

    public companion object {
        public fun from(
            route: NavKey,
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
    override val route: NavKey,
    override val labelRes: StringResource,
    override val selectedIcon: ImageVector,
    override val unselectedIcon: ImageVector,
    override val contentDescriptionRes: StringResource,
) : TopLevelDestination

