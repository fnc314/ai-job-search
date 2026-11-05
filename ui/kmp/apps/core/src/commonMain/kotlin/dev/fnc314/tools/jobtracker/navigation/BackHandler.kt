package dev.fnc314.tools.jobtracker.navigation

import androidx.compose.runtime.Composable

@Composable
internal expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
