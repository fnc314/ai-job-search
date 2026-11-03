package dev.fnc314.tools.jobtracker.util

import dev.fnc314.tools.jobtracker.data.model.*

/** Generic display name for any enum: converts SNAKE_CASE to "Snake Case". */
internal fun <T : Enum<T>> T.displayName(): String =
    name.split("_").joinToString(" ") {
        it.lowercase().replaceFirstChar { c -> c.uppercase() }
    }

/** TrackerStatus overrides the generic display name for DRAFTING → "Lead". */
internal fun TrackerStatus.displayName(): String =
    when (this) {
        TrackerStatus.DRAFTING -> "Lead"
        else -> displayName<TrackerStatus>()
    }
