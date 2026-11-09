package dev.fnc314.tools.jobtracker.app.core

import androidx.compose.runtime.Composable

@Composable
public expect fun KmpInspector(enabled: Boolean, content: @Composable () -> Unit)
