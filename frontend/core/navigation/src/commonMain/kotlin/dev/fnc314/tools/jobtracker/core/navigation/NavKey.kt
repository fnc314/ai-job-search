package dev.fnc314.tools.jobtracker.core.navigation

import androidx.navigation3.runtime.NavKey as AndroidxNavKey
import org.jetbrains.compose.resources.StringResource

/**
 * App-wide route contract extending Navigation 3 NavKey for destination routes.
 */
public interface NavKey : AndroidxNavKey {
    public val titleRes: StringResource
}
