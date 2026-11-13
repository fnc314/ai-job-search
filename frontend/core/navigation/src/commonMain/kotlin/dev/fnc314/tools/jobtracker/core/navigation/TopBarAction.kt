package dev.fnc314.tools.jobtracker.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Describes the toolbar icon treatments a [JobApplicationsTrackerNavKey] destination
 * requests from the app-level [AppScaffold].
 *
 * @property showSettingsIcon Whether to render the settings gear icon in the top-bar actions.
 * @property showBackButton Whether to render the back arrow in the navigation icon slot.
 *   When `false`, the navigation icon slot is left empty.
 * @property hasCustomActions Whether this destination contributes dynamic, screen-level
 *   actions (e.g., a Save button that appears only when content is loaded). When `true`,
 *   the [LocalTopBarActionsSlot] is exposed so the screen can populate its actions.
 */
public data class TopBarActions(
    val showSettingsIcon: Boolean = false,
    val showBackButton: Boolean = true,
    val hasCustomActions: Boolean = false,
) {
    public companion object {
        /** No toolbar actions — clears any that existed for the previous destination. */
        public val None: TopBarActions = TopBarActions()

        /** Default for top-level destinations: show settings icon, no back button. */
        public val TopLevel: TopBarActions = TopBarActions(showSettingsIcon = true, showBackButton = false)

        /** Default for detail destinations: show back button, no settings icon. */
        public val Detail: TopBarActions = TopBarActions(showBackButton = true)

        /** Detail destination with dynamic custom actions (e.g., artifact preview save button). */
        public val DetailWithCustomActions: TopBarActions =
            TopBarActions(showBackButton = true, hasCustomActions = true)
    }
}

/**
 * Wrapper holding a state-backed composable lambda that renders top-bar action buttons.
 * Using [MutableState] ensures Compose tracks changes to the render lambda and triggers
 * recomposition of the TopAppBar when child screens populate their actions.
 * Using a class instead of a bare function type avoids Kotlin compiler issues
 * with the `provides` infix operator on CompositionLocal.
 */
public class TopBarActionsSlot {
    public val render: MutableState<@Composable () -> Unit> = mutableStateOf({})
}

/**
 * CompositionLocal carrying the [TopBarActionsSlot] for child screens to populate.
 *
 * The [AppScaffold] provides this slot; child screens assign their actions composable
 * to `LocalTopBarActionsSlot.current.render` to contribute buttons to the shared TopAppBar,
 * eliminating the need for individual screens to embed their own Scaffold.
 */
public val LocalTopBarActionsSlot: androidx.compose.runtime.ProvidableCompositionLocal<TopBarActionsSlot> =
    staticCompositionLocalOf { TopBarActionsSlot() }
