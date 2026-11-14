package dev.fnc314.tools.jobtracker.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.materialkolor.MaterialKolors
import com.materialkolor.ktx.rememberDynamicScheme

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
    SetupSystemBars(darkTheme = darkTheme)

    val scheme = rememberDynamicScheme(seedColor = seedColor, isDark = darkTheme)
    val kolors = remember(scheme) { MaterialKolors(scheme) }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = kolors.primary(),
            onPrimary = kolors.onPrimary(),
            primaryContainer = kolors.primaryContainer(),
            onPrimaryContainer = kolors.onPrimaryContainer(),
            secondary = kolors.secondary(),
            onSecondary = kolors.onSecondary(),
            secondaryContainer = kolors.secondaryContainer(),
            onSecondaryContainer = kolors.onSecondaryContainer(),
            tertiary = kolors.tertiary(),
            onTertiary = kolors.onTertiary(),
            tertiaryContainer = kolors.tertiaryContainer(),
            onTertiaryContainer = kolors.onTertiaryContainer(),
            error = kolors.error(),
            onError = kolors.onError(),
            errorContainer = kolors.errorContainer(),
            onErrorContainer = kolors.onErrorContainer(),
            background = kolors.background(),
            onBackground = kolors.onBackground(),
            surface = kolors.surface(),
            onSurface = kolors.onSurface(),
            surfaceDim = kolors.surfaceDim(),
            surfaceBright = kolors.surfaceBright(),
            surfaceContainerLowest = kolors.surfaceContainerLowest(),
            surfaceContainerLow = kolors.surfaceContainerLow(),
            surfaceContainer = kolors.surfaceContainer(),
            surfaceContainerHigh = kolors.surfaceContainerHigh(),
            surfaceContainerHighest = kolors.surfaceContainerHighest(),
            onSurfaceVariant = kolors.onSurfaceVariant(),
            outline = kolors.outline(),
            outlineVariant = kolors.outlineVariant(),
            inverseSurface = kolors.inverseSurface(),
            inverseOnSurface = kolors.inverseOnSurface(),
            inversePrimary = kolors.inversePrimary(),
            scrim = kolors.scrim(),
            surfaceTint = kolors.surfaceTint(),
            surfaceVariant = kolors.surfaceVariant(),
        )
    } else {
        lightColorScheme(
            primary = kolors.primary(),
            onPrimary = kolors.onPrimary(),
            primaryContainer = kolors.primaryContainer(),
            onPrimaryContainer = kolors.onPrimaryContainer(),
            secondary = kolors.secondary(),
            onSecondary = kolors.onSecondary(),
            secondaryContainer = kolors.secondaryContainer(),
            onSecondaryContainer = kolors.onSecondaryContainer(),
            tertiary = kolors.tertiary(),
            onTertiary = kolors.onTertiary(),
            tertiaryContainer = kolors.tertiaryContainer(),
            onTertiaryContainer = kolors.onTertiaryContainer(),
            error = kolors.error(),
            onError = kolors.onError(),
            errorContainer = kolors.errorContainer(),
            onErrorContainer = kolors.onErrorContainer(),
            background = kolors.background(),
            onBackground = kolors.onBackground(),
            surface = kolors.surface(),
            onSurface = kolors.onSurface(),
            surfaceDim = kolors.surfaceDim(),
            surfaceBright = kolors.surfaceBright(),
            surfaceContainerLowest = kolors.surfaceContainerLowest(),
            surfaceContainerLow = kolors.surfaceContainerLow(),
            surfaceContainer = kolors.surfaceContainer(),
            surfaceContainerHigh = kolors.surfaceContainerHigh(),
            surfaceContainerHighest = kolors.surfaceContainerHighest(),
            onSurfaceVariant = kolors.onSurfaceVariant(),
            outline = kolors.outline(),
            outlineVariant = kolors.outlineVariant(),
            inverseSurface = kolors.inverseSurface(),
            inverseOnSurface = kolors.inverseOnSurface(),
            inversePrimary = kolors.inversePrimary(),
            scrim = kolors.scrim(),
            surfaceTint = kolors.surfaceTint(),
            surfaceVariant = kolors.surfaceVariant(),
        )
    }

    CompositionLocalProvider(
        LocalSpacing provides spacing,
        LocalIconSizes provides IconSizes(),
        LocalComponentSizes provides ComponentSizes(),
    ) {
        MaterialTheme(
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
