package dev.fnc314.tools.jobtracker

import androidx.compose.runtime.Composable

@Composable
public actual fun KmpInspector(content: @Composable () -> Unit) {
    com.waqas028.kmpinspector.KmpInspector(content = content)
}
