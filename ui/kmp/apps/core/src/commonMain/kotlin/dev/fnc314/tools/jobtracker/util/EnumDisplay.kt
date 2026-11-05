package dev.fnc314.tools.jobtracker.util

/** Generic display name for any enum: converts SNAKE_CASE to "Snake Case". */
internal fun <T : Enum<T>> T.displayName(): String =
    name.split("_").joinToString(" ") {
        it.lowercase().replaceFirstChar { c -> c.uppercase() }
    }
