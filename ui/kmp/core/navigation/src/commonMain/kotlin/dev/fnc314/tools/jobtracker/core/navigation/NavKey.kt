package dev.fnc314.tools.jobtracker.core.navigation

import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource

/**
 * App-wide route contract extending NavKey for destination routes.
 */
public interface NavKey {
    public val titleRes: StringResource
}
