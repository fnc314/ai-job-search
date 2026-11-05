package dev.fnc314.tools.jobtracker.navigation

import androidx.compose.runtime.Composable

@Composable
internal actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op on WasmJs
}
