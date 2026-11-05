package dev.fnc314.tools.jobtracker.core.data

import dev.fnc314.tools.jobtracker.core.data.database.DatabaseConverters
import dev.fnc314.tools.jobtracker.core.data.database.DatabaseHolder
import dev.fnc314.tools.jobtracker.core.data.database.JobApplicationMapper
import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.model.Uuid7
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
public class RoomDatabaseMappingTest {

    private val converters = DatabaseConverters()

    @Test
    public fun testUuid7_MonotonicityAndGeneration(): Unit {
        val id1 = Uuid7.generate()
        val id2 = Uuid7.generate()

        assertNotNull(id1.toString())
        assertEquals(36, id1.toString().length)
        assertTrue(id1 <= id2, "UUIDv7 should be monotonically increasing or equal")

        val parsed = Uuid7.parse(id1.toString())
        assertEquals(id1, parsed)

        val nullParsed = Uuid7.parseOrNull("invalid-uuid")
        assertNull(nullParsed)
    }

    @Test
    public fun testDatabaseConverters_Uuid7RoundTrip(): Unit {
        val original = Uuid7.generate()
        val serialized = converters.fromUuid7(original)
        val deserialized = converters.toUuid7(serialized)

        assertEquals(original, deserialized)
        assertNull(converters.fromUuid7(null))
        assertNull(converters.toUuid7(null))
    }

    @Test
    public fun testDatabaseConverters_EnumsAndLists(): Unit {
        assertEquals("OFFER_EXPECTED", converters.fromTrackerStatus(TrackerStatus.OFFER_EXPECTED))
        assertEquals(TrackerStatus.OFFER_EXPECTED, converters.toTrackerStatus("OFFER_EXPECTED"))
        assertEquals(TrackerStatus.DRAFTING, converters.toTrackerStatus("UNKNOWN_NON_EXISTENT"))

        assertEquals("LINKEDIN", converters.fromApplicationChannel(ApplicationChannel.LINKEDIN))
        assertEquals(ApplicationChannel.LINKEDIN, converters.toApplicationChannel("LINKEDIN"))
        assertEquals(ApplicationChannel.UNSPECIFIED, converters.toApplicationChannel(null))

        val now = Instant.fromEpochMilliseconds(1730000000000L)
        val millis = converters.fromInstant(now)
        assertEquals(1730000000000L, millis)
        assertEquals(now, converters.toInstant(millis))

        val list = listOf("android", "kotlin", "architecture")
        val joined = converters.fromStringList(list)
        assertEquals("android,kotlin,architecture", joined)
        assertEquals(list, converters.toStringList(joined))
        assertTrue(converters.toStringList(null).isEmpty())
    }

    @Test
    public fun testJobApplicationMapper_BidirectionalRoundTrip(): Unit {
        val originalUuid = Uuid7.generate()
        val domain = Factory.createJobApplication(
            id = "2026-03-pnc-lead",
            company = "PNC Financial Services",
            role = "Principal Android Engineer",
            location = "Pittsburgh, PA",
            status = TrackerStatus.APPLIED,
            channel = ApplicationChannel.REFERRAL,
            fitRating = 95,
            tags = listOf("android", "kotlin", "architecture"),
            contactIds = listOf("contact-1"),
            uuid = originalUuid,
            schemaVersion = 7,
        )

        val entity = JobApplicationMapper.toEntity(domain)
        assertEquals(originalUuid, entity.uuid)
        assertEquals("2026-03-pnc-lead", entity.slugId)
        assertEquals(7, entity.schemaVersion)
        assertEquals("PNC Financial Services", entity.company)
        assertEquals(TrackerStatus.APPLIED, entity.status)
        assertEquals(95, entity.fitScore)

        val reconstructed = JobApplicationMapper.toDomain(entity)
        assertEquals(domain.id, reconstructed.id)
        assertEquals(domain.uuid, reconstructed.uuid)
        assertEquals(domain.schemaVersion, reconstructed.schemaVersion)
        assertEquals(domain.company, reconstructed.company)
        assertEquals(domain.role, reconstructed.role)
        assertEquals(domain.status, reconstructed.status)
        assertEquals(domain.channel, reconstructed.channel)
        assertEquals(domain.fitScore, reconstructed.fitScore)
        assertEquals(domain.tags, reconstructed.tags)
    }

    @Test
    public fun testDatabaseHolder_NullDatabaseGracefulHandling(): Unit {
        val holder = DatabaseHolder(null)
        assertNull(holder.database)
        assertNull(holder.dao)
    }
}
