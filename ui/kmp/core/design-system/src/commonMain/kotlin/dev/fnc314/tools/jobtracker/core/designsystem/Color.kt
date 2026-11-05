package dev.fnc314.tools.jobtracker.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

public object SeedColors {
    public const val DEFAULT_SEED_HEX: String = "#2F5597"

    public val ProfessionalBlue: Color = Color(0xFF2F5597)
    public val MaterialPurple: Color = Color(0xFF6750A4)
    public val ForestTeal: Color = Color(0xFF006A6A)
    public val SlateIndigo: Color = Color(0xFF3C5BA9)
    public val NordicSage: Color = Color(0xFF386A20)
    public val WarmAmber: Color = Color(0xFF825500)

    public val All: List<Pair<String, Color>> = listOf(
        "Professional Blue" to ProfessionalBlue,
        "Material Purple" to MaterialPurple,
        "Forest Teal" to ForestTeal,
        "Slate Indigo" to SlateIndigo,
        "Nordic Sage" to NordicSage,
        "Warm Amber" to WarmAmber,
    )
}

/**
 * Parses a hex string (e.g. "#2F5597" or "2F5597") into a Compose [Color], or null if invalid.
 */
public fun String.toColorOrNull(): Color? {
    return try {
        val hex = this.removePrefix("#")
        if (hex.length == 6) {
            val r = hex.substring(0, 2).toInt(16)
            val g = hex.substring(2, 4).toInt(16)
            val b = hex.substring(4, 6).toInt(16)
            Color(red = r / 255f, green = g / 255f, blue = b / 255f)
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Formats a Compose [Color] into a standard 6-digit hex string (e.g. "#2f5597").
 */
public fun Color.toHexString(): String {
    val r = (this.red * 255).toInt().coerceIn(0, 255)
    val g = (this.green * 255).toInt().coerceIn(0, 255)
    val b = (this.blue * 255).toInt().coerceIn(0, 255)
    return "#" +
            r.toString(16).padStart(2, '0') +
            g.toString(16).padStart(2, '0') +
            b.toString(16).padStart(2, '0')
}

/**
 * Derives a full M3 Light ColorScheme using the provided seed color.
 */
public fun buildLightColorScheme(seed: Color): ColorScheme {
    // Generate harmonious tonal shifts based on seed
    val isLightSeed = seed.luminance() > 0.5f
    val onPrimary = if (isLightSeed) Color.Black else Color.White
    
    // Tonal containers
    val primaryContainer = seed.copy(alpha = 0.16f)
    val onPrimaryContainer = seed

    return lightColorScheme(
        primary = seed,
        onPrimary = onPrimary,
        primaryContainer = Color(
            red = (seed.red * 0.2f + 0.8f).coerceIn(0f, 1f),
            green = (seed.green * 0.2f + 0.8f).coerceIn(0f, 1f),
            blue = (seed.blue * 0.2f + 0.8f).coerceIn(0f, 1f),
        ),
        onPrimaryContainer = Color(
            red = (seed.red * 0.6f).coerceIn(0f, 1f),
            green = (seed.green * 0.6f).coerceIn(0f, 1f),
            blue = (seed.blue * 0.6f).coerceIn(0f, 1f),
        ),
        secondary = Color(
            red = (seed.red * 0.7f + 0.1f).coerceIn(0f, 1f),
            green = (seed.green * 0.7f + 0.1f).coerceIn(0f, 1f),
            blue = (seed.blue * 0.7f + 0.2f).coerceIn(0f, 1f),
        ),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE8DEF8),
        onSecondaryContainer = Color(0xFF1D192B),
        tertiary = Color(0xFF7D5260),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFD8E4),
        onTertiaryContainer = Color(0xFF31111D),
        background = Color(0xFFFEF7FF),
        onBackground = Color(0xFF1D1B20),
        surface = Color(0xFFFEF7FF),
        onSurface = Color(0xFF1D1B20),
        surfaceVariant = Color(0xFFE7E0EC),
        onSurfaceVariant = Color(0xFF49454F),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF7F2FA),
        surfaceContainer = Color(0xFFF3EDF7),
        surfaceContainerHigh = Color(0xFFECE6F0),
        surfaceContainerHighest = Color(0xFFE6E0E9),
        outline = Color(0xFF79747E),
        outlineVariant = Color(0xFFCAC4D0),
        error = Color(0xFFB3261E),
        onError = Color.White,
        errorContainer = Color(0xFFF9DEDC),
        onErrorContainer = Color(0xFF410E0B),
    )
}

/**
 * Derives a full M3 Dark ColorScheme using the provided seed color.
 */
public fun buildDarkColorScheme(seed: Color): ColorScheme {
    val primaryLightened = Color(
        red = (seed.red * 0.5f + 0.5f).coerceIn(0f, 1f),
        green = (seed.green * 0.5f + 0.5f).coerceIn(0f, 1f),
        blue = (seed.blue * 0.5f + 0.5f).coerceIn(0f, 1f),
    )

    return darkColorScheme(
        primary = primaryLightened,
        onPrimary = Color(0xFF1D192B),
        primaryContainer = Color(
            red = (seed.red * 0.5f).coerceIn(0f, 1f),
            green = (seed.green * 0.5f).coerceIn(0f, 1f),
            blue = (seed.blue * 0.5f).coerceIn(0f, 1f),
        ),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFFCCC2DC),
        onSecondary = Color(0xFF332D41),
        secondaryContainer = Color(0xFF4A4458),
        onSecondaryContainer = Color(0xFFE8DEF8),
        tertiary = Color(0xFFEFB8C8),
        onTertiary = Color(0xFF492532),
        tertiaryContainer = Color(0xFF633B48),
        onTertiaryContainer = Color(0xFFFFD8E4),
        background = Color(0xFF141218),
        onBackground = Color(0xFFE6E0E9),
        surface = Color(0xFF141218),
        onSurface = Color(0xFFE6E0E9),
        surfaceVariant = Color(0xFF49454F),
        onSurfaceVariant = Color(0xFFCAC4D0),
        surfaceContainerLowest = Color(0xFF0F0D13),
        surfaceContainerLow = Color(0xFF1D1B20),
        surfaceContainer = Color(0xFF211F26),
        surfaceContainerHigh = Color(0xFF2B2930),
        surfaceContainerHighest = Color(0xFF36343B),
        outline = Color(0xFF938F99),
        outlineVariant = Color(0xFF49454F),
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
        errorContainer = Color(0xFF8C1D18),
        onErrorContainer = Color(0xFFF9DEDC),
    )
}
