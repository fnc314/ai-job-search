package dev.fnc314.tools.jobtracker.core.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Predefined palette of canonical seed colors used for Material 3 dynamic color scheme derivation.
 *
 * Adheres strictly to Architecture Rule 3: Zero rogue `Color()` calls outside this file.
 */
public object SeedColors {
    /** The default primary theme seed hexadecimal color code (`"#2F5597"`). */
    public const val DEFAULT_SEED_HEX: String = "#2F5597"

    /** Canonical Professional Blue seed color (`#2F5597`). */
    public val ProfessionalBlue: Color = Color(0xFF2F5597)

    /** Standard Material 3 purple seed color (`#6750A4`). */
    public val MaterialPurple: Color = Color(0xFF6750A4)

    /** Forest Teal seed color (`#006A6A`). */
    public val ForestTeal: Color = Color(0xFF006A6A)

    /** Slate Indigo seed color (`#3C5BA9`). */
    public val SlateIndigo: Color = Color(0xFF3C5BA9)

    /** Nordic Sage seed color (`#386A20`). */
    public val NordicSage: Color = Color(0xFF386A20)

    /** Warm Amber seed color (`#825500`). */
    public val WarmAmber: Color = Color(0xFF825500)

    /**
     * Complete list of available seed colors paired with their human-readable preset names.
     */
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
 * Parses a 6-digit hex string (e.g. `"#2F5597"` or `"2F5597"`) into a Compose [Color], or null if invalid.
 *
 * @receiver The hexadecimal string to parse.
 * @return The parsed [Color] or null if the input format is invalid.
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
 * Formats a Compose [Color] into a standard 6-digit lower-case hex string prefixed with `#` (e.g. `"#2f5597"`).
 *
 * @receiver The [Color] instance to format.
 * @return Canonical hex code string.
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

