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
 * Extracts the RGB channels of this [Color] as 0–255 integers.
 *
 * @return An array of size 3: `[R, G, B]`.
 */
public fun Color.toRgbChannels(): IntArray {
    return intArrayOf(
        (this.red * 255).toInt().coerceIn(0, 255),
        (this.green * 255).toInt().coerceIn(0, 255),
        (this.blue * 255).toInt().coerceIn(0, 255),
    )
}

/**
 * Extracts the RGBA channels of this [Color] as 0–255 integers.
 *
 * @return An array of size 4: `[R, G, B, A]`.
 */
public fun Color.toRgbaChannels(): IntArray {
    return intArrayOf(
        (this.red * 255).toInt().coerceIn(0, 255),
        (this.green * 255).toInt().coerceIn(0, 255),
        (this.blue * 255).toInt().coerceIn(0, 255),
        (this.alpha * 255).toInt().coerceIn(0, 255),
    )
}

/**
 * Creates a [Color] from an integer array of channel values (0–255).
 *
 * Supports arrays of size 3 (`[R, G, B]` — alpha defaults to 255) or size 4 (`[R, G, B, A]`).
 *
 * @throw IllegalArgumentException if the array size is not 3 or 4.
 */
public fun IntArray.toColor(): Color = when (size) {
    3 -> Color(
        red = this[0] / 255f,
        green = this[1] / 255f,
        blue = this[2] / 255f,
    )
    4 -> Color(
        red = this[0] / 255f,
        green = this[1] / 255f,
        blue = this[2] / 255f,
        alpha = this[3] / 255f,
    )
    else -> throw IllegalArgumentException("IntArray size must be 3 (RGB) or 4 (RGBA), got $size")
}

/**
 * Parses a hex string into a Compose [Color] with alpha support, or null if invalid.
 *
 * Accepts `"#RRGGBB"` (alpha defaults to 255) or `"#AARRGGBB"` formats.
 *
 * @receiver The hexadecimal string to parse.
 * @return The parsed [Color] with alpha, or null if the input format is invalid.
 */
public fun String.toColorWithAlphaOrNull(): Color? {
    return try {
        val hex = this.removePrefix("#")
        when (hex.length) {
            6 -> {
                val r = hex.substring(0, 2).toInt(16)
                val g = hex.substring(2, 4).toInt(16)
                val b = hex.substring(4, 6).toInt(16)
                Color(red = r / 255f, green = g / 255f, blue = b / 255f)
            }
            8 -> {
                val a = hex.substring(0, 2).toInt(16)
                val r = hex.substring(2, 4).toInt(16)
                val g = hex.substring(4, 6).toInt(16)
                val b = hex.substring(6, 8).toInt(16)
                Color(red = r / 255f, green = g / 255f, blue = b / 255f, alpha = a / 255f)
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * Formats a Compose [Color] into a hex string prefixed with `#`.
 *
 * When [includeAlpha] is `false` (default), produces `"#rrggbb"`.
 * When [includeAlpha] is `true`, produces `"#aarrggbb"`.
 *
 * @receiver The [Color] instance to format.
 * @param includeAlpha Whether to prepend the alpha channel to the hex string.
 * @return Canonical hex code string.
 */
public fun Color.toHexString(includeAlpha: Boolean = false): String {
    val r = (this.red * 255).toInt().coerceIn(0, 255)
    val g = (this.green * 255).toInt().coerceIn(0, 255)
    val b = (this.blue * 255).toInt().coerceIn(0, 255)
    val rgbHex = r.toString(16).padStart(2, '0') +
            g.toString(16).padStart(2, '0') +
            b.toString(16).padStart(2, '0')
    return if (includeAlpha) {
        val a = (this.alpha * 255).toInt().coerceIn(0, 255)
        "#" + a.toString(16).padStart(2, '0') + rgbHex
    } else {
        "#" + rgbHex
    }
}

