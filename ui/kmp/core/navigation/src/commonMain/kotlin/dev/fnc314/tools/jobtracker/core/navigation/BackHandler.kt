package dev.fnc314.tools.jobtracker.core.navigation

import androidx.compose.runtime.Composable

@Composable
public expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
