package dev.fnc314.tools.jobtracker.core.data.model

/** Generic display name fallback for any enum: converts SNAKE_CASE to "Snake Case". */
public fun <T : Enum<T>> T.displayName(): String =
    name.split("_").joinToString(" ") {
        it.lowercase().replaceFirstChar { c -> c.uppercase() }
    }
