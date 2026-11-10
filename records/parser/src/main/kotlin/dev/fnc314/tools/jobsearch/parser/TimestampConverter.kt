package dev.fnc314.tools.jobsearch.parser

import com.google.protobuf.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Utility for converting between ISO-8601 calendar date strings and Google Protocol Buffer [Timestamp] objects.
 *
 * Uses [java.time.LocalDate] and [java.time.Instant] under UTC to perform robust conversions between epoch seconds
 * and ISO date strings. Supports recursive map and collection traversal for batch document serialization.
 */
public object TimestampConverter {

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
        val localDate = LocalDate.parse(datePart)
        val epochSeconds = localDate.atStartOfDay(ZoneOffset.UTC).toEpochSecond()
        return Timestamp.newBuilder()
            .setSeconds(epochSeconds)
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
        val instant = Instant.ofEpochSecond(ts.seconds, ts.nanos.toLong())
        return instant.atZone(ZoneOffset.UTC).toLocalDate().toString()
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
}
