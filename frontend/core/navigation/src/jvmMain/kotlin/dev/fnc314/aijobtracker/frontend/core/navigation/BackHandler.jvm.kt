package dev.fnc314.aijobtracker.frontend.core.navigation

import androidx.compose.runtime.Composable

/** Actual platform implementation of [BackHandler]. */
@Composable
public actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op on JVM desktop
}
