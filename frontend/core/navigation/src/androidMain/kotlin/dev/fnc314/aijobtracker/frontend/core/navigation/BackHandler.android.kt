package dev.fnc314.aijobtracker.frontend.core.navigation

import androidx.activity.compose.BackHandler as AndroidBackHandler
import androidx.compose.runtime.Composable

/** Actual platform implementation of [BackHandler]. */
@Composable
public actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    AndroidBackHandler(enabled = enabled, onBack = onBack)
}
