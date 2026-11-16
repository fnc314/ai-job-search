package dev.fnc314.aijobtracker.frontend.app.core

import androidx.compose.runtime.Composable

/**
 * Diagnostic inspector overlay composable wrapping [content].
 *
 * @param enabled Whether inspection tools and overlays are active.
 * @param content Target UI hierarchy to inspect.
 */
@Composable
public expect fun KmpInspector(enabled: Boolean, content: @Composable () -> Unit)

/**
 * Sets the global inspector enabled state.
 * No-op on platforms where the inspector is not available.
 */
public expect fun setInspectorEnabled(enabled: Boolean)
