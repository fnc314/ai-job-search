package dev.fnc314.tools.jobtracker.util

import dev.fnc314.tools.jobtracker.core.data.model.*

/** Generic display name for any enum: converts SNAKE_CASE to "Snake Case". */
internal fun <T : Enum<T>> T.displayName(): String =
    name.split("_").joinToString(" ") {
        it.lowercase().replaceFirstChar { c -> c.uppercase() }
    }

/** TrackerStatus display name delegates to the member method. */
internal fun TrackerStatus.displayName(): String = this.displayName
