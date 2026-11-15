package dev.fnc314.tools.jobtracker.core.navigation

import androidx.compose.runtime.Composable

/** Actual platform implementation of [BackHandler]. */
@Composable
public actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op on iOS
}
