package dev.fnc314.tools.jobtracker.core.navigation

import org.jetbrains.compose.resources.StringResource

/**
 * Base identity interface for navigation destinations across all platforms.
 */
public interface NavKey

/**
 * App-wide route contract extending NavKey for destination routes.
 */
public interface NavKeyRoute : NavKey {
    public val titleRes: StringResource
}
