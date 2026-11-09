package dev.fnc314.tools.jobtracker.core.data.util

import kotlin.time.Instant

/**
 * Utility parser converting known multiplatform raw types to [Instant].
 *
 * If the input is null, missing, or unparseable, returns the supplied default
 * (by default [Instant.DISTANT_PAST]). Platform SDK timestamp types are converted at the
 * platform boundary (`RemoteDataSource.*`), so no string-shape assumptions are made here.
 */
public object InstantParser {
    private val DATE_ONLY_REGEX = Regex("""^\d{4}-\d{2}-\d{2}$""")
    private const val EPOCH_MILLIS_THRESHOLD = 100_000_000_000L

    /**
     * Parses [raw] into an [Instant].
     *
     * Supports [Instant], numeric epoch seconds/milliseconds, ISO-8601 strings, and `YYYY-MM-DD` strings.
     *
     * @param raw Raw value from a remote record field.
     * @param default Value returned when [raw] is absent or cannot be parsed.
     */
    public fun parse(raw: Any?, default: Instant = Instant.DISTANT_PAST): Instant = when (raw) {
        is Instant -> raw
        is Number -> raw.toLong().let {
            if (it < EPOCH_MILLIS_THRESHOLD) Instant.fromEpochSeconds(it) else Instant.fromEpochMilliseconds(it)
        }
        is String -> parseString(raw.trim(), default)
        else -> default
    }

    private fun parseString(value: String, default: Instant): Instant {
        if (value.isEmpty()) return default
        val iso = if (DATE_ONLY_REGEX.matches(value)) "${value}T00:00:00Z" else value
        return runCatching { Instant.parse(iso) }.getOrDefault(default)
    }
}
