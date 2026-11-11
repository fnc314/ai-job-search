package dev.fnc314.tools.jobtracker.app.core

import androidx.compose.runtime.Composable
import com.waqas028.kmpinspector.Inspector
import com.waqas028.kmpinspector.KmpInspector as LibKmpInspector

@Composable
public actual fun KmpInspector(enabled: Boolean, content: @Composable () -> Unit) {
    LibKmpInspector(enabled = enabled) {
        content()
    }
}

public actual fun setInspectorEnabled(enabled: Boolean) {
    Inspector.enabled = enabled
}
