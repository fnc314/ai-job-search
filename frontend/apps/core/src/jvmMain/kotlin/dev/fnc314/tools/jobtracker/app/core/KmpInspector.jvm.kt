package dev.fnc314.tools.jobtracker.app.core

import androidx.compose.runtime.Composable

@Composable
public actual fun KmpInspector(enabled: Boolean, content: @Composable () -> Unit) {
  content()
}
