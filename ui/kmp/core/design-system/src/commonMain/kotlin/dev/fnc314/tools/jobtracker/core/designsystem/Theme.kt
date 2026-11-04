package dev.fnc314.tools.jobtracker.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun JobTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    seedColor: Color = Color(0xFF6750A4),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = seedColor,
        )
    } else {
        lightColorScheme(
            primary = seedColor,
        )
    }

    SetupSystemBars(darkTheme = darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}

@Composable
expect fun SetupSystemBars(darkTheme: Boolean)
