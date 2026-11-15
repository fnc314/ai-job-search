package dev.fnc314.tools.jobtracker.core.navigation

import androidx.compose.runtime.Composable

/**
 * Cross-platform back press interception handler.
 *
 * @param enabled Whether back handling is active.
 * @param onBack Callback invoked when a back event is triggered.
 */
@Composable
public expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
