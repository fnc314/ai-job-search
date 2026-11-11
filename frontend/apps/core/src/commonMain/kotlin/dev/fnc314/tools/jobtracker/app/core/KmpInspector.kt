package dev.fnc314.tools.jobtracker.app.core

import androidx.compose.runtime.Composable

@Composable
public expect fun KmpInspector(enabled: Boolean, content: @Composable () -> Unit)

/**
 * Sets the global inspector enabled state.
 * No-op on platforms where the inspector is not available.
 */
public expect fun setInspectorEnabled(enabled: Boolean)
