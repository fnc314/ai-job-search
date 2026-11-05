package dev.fnc314.tools.jobsearch.parser

import com.google.protobuf.Timestamp

/**
 * Utility for converting between ISO-8601 calendar date strings and Google Protocol Buffer [Timestamp] objects.
 *
 * Implements civil calendar arithmetic algorithms to perform zero-dependency conversions between epoch seconds
 * and year/month/day components. Supports recursive map and collection traversal for batch document serialization.
 */
public object TimestampConverter {

    private const val SECONDS_IN_DAY: Long = 86400L

    /**
     * Parses an ISO-8601 calendar date or timestamp string into a protobuf [Timestamp].
     *
     * Handles formats such as `"YYYY-MM-DD"` and `"YYYY-MM-DDTHH:MM:SS"`.
     *
     * @param iso The ISO date/time string.
     * @return The corresponding protobuf [Timestamp] with seconds resolution.
     */
    public fun isoToTimestamp(iso: String): Timestamp {
        val datePart = iso.substringBefore('T')
        val parts = datePart.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()
        
        val epochDays = daysFromCivil(year, month, day)
        val seconds = epochDays * SECONDS_IN_DAY
        
        return Timestamp.newBuilder()
            .setSeconds(seconds)
            .setNanos(0)
            .build()
    }

    /**
     * Formats a protobuf [Timestamp] into an ISO-8601 date string (`"YYYY-MM-DD"`).
     *
     * @param ts The protobuf [Timestamp] instance.
     * @return The formatted calendar date string.
     */
    public fun timestampToIso(ts: Timestamp): String {
        val epochDays = ts.seconds / SECONDS_IN_DAY
        val (year, month, day) = civilFromDays(epochDays)
        return String.format("%04d-%02d-%02d", year, month, day)
    }

    /**
     * Recursively transforms data structures, replacing ISO date strings with protobuf [Timestamp] instances.
     *
     * Traverses [Map] and [List] containers recursively, leaving non-date scalar values unchanged.
     *
     * @param value The object, collection, or scalar to transform.
     * @return The transformed structure suitable for protobuf consumption.
     */
    public fun toFirestore(value: Any?): Any? {
        return when (value) {
            is String -> {
                if (value.matches(Regex("\\d{4}-\\d{2}-\\d{2}.*"))) {
                    isoToTimestamp(value)
                } else {
                    value
                }
            }
            is Map<*, *> -> value.mapValues { toFirestore(it.value) }
            is List<*> -> value.map { toFirestore(it) }
            else -> value
        }
    }

    /**
     * Recursively transforms data structures, replacing protobuf [Timestamp] instances with ISO date strings.
     *
     * Traverses [Map] and [List] containers recursively, converting timestamps for JSON / Python serialization.
     *
     * @param value The object, collection, or scalar to transform.
     * @return The transformed structure with ISO date strings.
     */
    public fun fromFirestore(value: Any?): Any? {
        return when (value) {
            is Timestamp -> timestampToIso(value)
            is Map<*, *> -> value.mapValues { fromFirestore(it.value) }
            is List<*> -> value.map { fromFirestore(it) }
            else -> value
        }
    }

    private fun daysFromCivil(y: Int, m: Int, d: Int): Long {
        val ym = 12L * y + maxOf(0, m - 3)
        return (365L * ym + ym / 4 - ym / 100 + ym / 400 + ((15L * (m + 9)) / 4) + d - 719469L)
    }

    private fun civilFromDays(z: Long): Triple<Int, Int, Int> {
        val adjustedZ = z + 719469L
        val era = if (adjustedZ >= 0) adjustedZ else (adjustedZ - 146096L) / 146097L
        val doe = adjustedZ - era * 146097L
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = mp + if (mp < 10) 3 else -9
        val year = y + if (m <= 2) 1 else 0
        return Triple(year.toInt(), m.toInt(), d.toInt())
    }
}
