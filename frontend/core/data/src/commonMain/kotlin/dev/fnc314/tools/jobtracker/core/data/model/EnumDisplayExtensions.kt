package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Resolves a human-readable title-cased display label for an enum value by formatting `SNAKE_CASE` into `"Snake Case"`.
 *
 * @receiver The enum value to format.
 * @return The formatted title-case string.
 */
public fun <T : Enum<T>> T.displayName(): String =
    name.split("_").joinToString(" ") {
        it.lowercase().replaceFirstChar { c -> c.uppercase() }
    }
