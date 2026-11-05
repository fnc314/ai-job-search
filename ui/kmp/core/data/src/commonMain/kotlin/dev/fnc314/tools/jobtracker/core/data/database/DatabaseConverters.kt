package dev.fnc314.tools.jobtracker.core.data.database

import androidx.room3.ColumnTypeConverter
import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.model.Uuid7
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
public class DatabaseConverters {

    @ColumnTypeConverter
    public fun fromUuid7(uuid7: Uuid7?): String? = uuid7?.toString()

    @ColumnTypeConverter
    public fun toUuid7(value: String?): Uuid7? = Uuid7.parseOrNull(value)

    @ColumnTypeConverter
    public fun fromUuid(uuid: Uuid?): String? = uuid?.toString()

    @ColumnTypeConverter
    public fun toUuid(value: String?): Uuid? =
        if (value == null) null else runCatching { Uuid.parse(value) }.getOrNull()

    @ColumnTypeConverter
    public fun fromTrackerStatus(status: TrackerStatus?): String? = status?.name

    @ColumnTypeConverter
    public fun toTrackerStatus(value: String?): TrackerStatus =
        value?.let { runCatching { TrackerStatus.valueOf(it) }.getOrNull() } ?: TrackerStatus.DRAFTING

    @ColumnTypeConverter
    public fun fromApplicationChannel(channel: ApplicationChannel?): String? = channel?.name

    @ColumnTypeConverter
    public fun toApplicationChannel(value: String?): ApplicationChannel =
        value?.let { runCatching { ApplicationChannel.valueOf(it) }.getOrNull() } ?: ApplicationChannel.UNSPECIFIED

    @ColumnTypeConverter
    public fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilliseconds()

    @ColumnTypeConverter
    public fun toInstant(millis: Long?): Instant? =
        millis?.let { Instant.fromEpochMilliseconds(it) }

    @ColumnTypeConverter
    public fun fromStringList(list: List<String>?): String? =
        list?.joinToString(",")

    @ColumnTypeConverter
    public fun toStringList(value: String?): List<String> =
        if (value.isNullOrBlank()) emptyList() else value.split(",")
}
