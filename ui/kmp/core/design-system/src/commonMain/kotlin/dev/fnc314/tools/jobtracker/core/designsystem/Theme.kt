package dev.fnc314.tools.jobtracker.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/**
 * Root theme composable for the Job Tracker application.
 *
 * Configures Material 3 dynamic color schemes derived from [seedColor], attaches [JobTrackerTypography]
 * and [JobTrackerShapes], configures platform system bars, and provides [LocalSpacing], [LocalIconSizes],
 * and [LocalComponentSizes] down the composition hierarchy.
 *
 * @param darkTheme Whether to render dark mode color schemes. Defaults to [isSystemInDarkTheme].
 * @param seedColor Base anchor seed color used for color scheme derivation. Defaults to [SeedColors.ProfessionalBlue].
 * @param spacing Grid spacing token set. Defaults to [Spacing].
 * @param content The composable UI tree to render inside the theme.
 */
@Composable
public fun JobTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    seedColor: Color = SeedColors.ProfessionalBlue,
    spacing: Spacing = Spacing(),
    content: @Composable () -> Unit,
): Unit {
    val colorScheme = if (darkTheme) {
        buildDarkColorScheme(seedColor)
    } else {
        buildLightColorScheme(seedColor)
    }

    SetupSystemBars(darkTheme = darkTheme)

    CompositionLocalProvider(
        LocalSpacing provides spacing,
        LocalIconSizes provides IconSizes(),
        LocalComponentSizes provides ComponentSizes(),
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = JobTrackerTypography,
            shapes = JobTrackerShapes,
            content = content,
        )
    }
}

/**
 * Platform-specific system bar customization (status bar, navigation bar scrim, icon contrast).
 *
 * Implemented via Android Activity window insets, Desktop window decorators, and iOS status bar styles.
 *
 * @param darkTheme Whether dark mode is active to ensure proper icon contrast.
 */
@Composable
public expect fun SetupSystemBars(darkTheme: Boolean): Unit
