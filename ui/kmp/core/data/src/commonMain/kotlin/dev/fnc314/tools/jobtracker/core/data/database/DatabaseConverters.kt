package dev.fnc314.tools.jobtracker.core.data.database

import androidx.room3.ColumnTypeConverter
import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.model.Uuid7
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Room 3 column type converters serializing custom domain types, enums, timestamps, and collections to SQLite column primitives.
 */
@OptIn(ExperimentalUuidApi::class)
public class DatabaseConverters {

    /** Serializes a [Uuid7] instance to a 36-character hexadecimal string or null. */
    @ColumnTypeConverter
    public fun fromUuid7(uuid7: Uuid7?): String? = uuid7?.toString()

    /** Deserializes a string representation into a [Uuid7] instance or null. */
    @ColumnTypeConverter
    public fun toUuid7(value: String?): Uuid7? = Uuid7.parseOrNull(value)

    /** Serializes a stdlib [Uuid] instance to a 36-character hexadecimal string or null. */
    @ColumnTypeConverter
    public fun fromUuid(uuid: Uuid?): String? = uuid?.toString()

    /** Deserializes a string representation into a stdlib [Uuid] instance or null. */
    @ColumnTypeConverter
    public fun toUuid(value: String?): Uuid? =
        if (value == null) null else runCatching { Uuid.parse(value) }.getOrNull()

    /** Serializes a [TrackerStatus] to its wire key string or null. */
    @ColumnTypeConverter
    public fun fromTrackerStatus(status: TrackerStatus?): String? = status?.wireKey

    /** Deserializes a string representation to a [TrackerStatus] constant, defaulting to [TrackerStatus.DRAFTING]. */
    @ColumnTypeConverter
    public fun toTrackerStatus(value: String?): TrackerStatus =
        TrackerStatus.fromWireKey(value)

    /** Serializes an [ApplicationChannel] to its wire key string or null. */
    @ColumnTypeConverter
    public fun fromApplicationChannel(channel: ApplicationChannel?): String? = channel?.wireKey

    /** Deserializes a string representation to an [ApplicationChannel] constant, defaulting to [ApplicationChannel.UNSPECIFIED]. */
    @ColumnTypeConverter
    public fun toApplicationChannel(value: String?): ApplicationChannel =
        ApplicationChannel.fromWireKey(value)

    /** Serializes a [kotlin.time.Instant] to epoch milliseconds integer or null. */
    @ColumnTypeConverter
    public fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilliseconds()

    /** Deserializes epoch milliseconds integer to a [kotlin.time.Instant] or null. */
    @ColumnTypeConverter
    public fun toInstant(millis: Long?): Instant? =
        millis?.let { Instant.fromEpochMilliseconds(it) }

    /** Serializes a list of strings into a comma-delimited string or null. */
    @ColumnTypeConverter
    public fun fromStringList(list: List<String>?): String? =
        list?.joinToString(",")

    /** Deserializes a comma-delimited string into a list of strings. */
    @ColumnTypeConverter
    public fun toStringList(value: String?): List<String> =
        if (value.isNullOrBlank()) emptyList() else value.split(",")
}
