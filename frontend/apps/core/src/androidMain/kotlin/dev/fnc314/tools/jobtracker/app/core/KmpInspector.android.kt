package dev.fnc314.tools.jobtracker.app.core

import androidx.compose.runtime.Composable
import com.waqas028.kmpinspector.Inspector

@Composable
public actual fun KmpInspector(enabled: Boolean, content: @Composable () -> Unit) {
  content()
}

public actual fun setInspectorEnabled(enabled: Boolean) {
  Inspector.enabled = enabled
}
