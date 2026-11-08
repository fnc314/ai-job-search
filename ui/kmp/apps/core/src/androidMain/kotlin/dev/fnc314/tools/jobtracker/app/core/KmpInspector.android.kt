package dev.fnc314.tools.jobtracker.app.core

import androidx.compose.runtime.Composable
import com.waqas028.kmpinspector.KmpInspector

@Composable
public actual fun KmpInspector(content: @Composable () -> Unit) {
  KmpInspector(content = content)
}
