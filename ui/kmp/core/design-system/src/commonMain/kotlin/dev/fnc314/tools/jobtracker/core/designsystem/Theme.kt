package dev.fnc314.tools.jobtracker.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun JobTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    seedColor: Color = SeedColors.ProfessionalBlue,
    spacing: Spacing = Spacing(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) {
        buildDarkColorScheme(seedColor)
    } else {
        buildLightColorScheme(seedColor)
    }

    SetupSystemBars(darkTheme = darkTheme)

    CompositionLocalProvider(
        LocalSpacing provides spacing
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = JobTrackerTypography,
            shapes = JobTrackerShapes,
            content = content,
        )
    }
}

@Composable
expect fun SetupSystemBars(darkTheme: Boolean)
