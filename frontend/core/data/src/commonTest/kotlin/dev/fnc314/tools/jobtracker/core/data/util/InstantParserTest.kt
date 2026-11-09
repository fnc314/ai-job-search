package dev.fnc314.tools.jobtracker.core.data.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class InstantParserTest {

    @Test
    fun parse_instantPassthrough() {
        val instant = Instant.fromEpochSeconds(1_731_089_200)
        assertEquals(instant, InstantParser.parse(instant))
    }

    @Test
    fun parse_epochSecondsAndMillis() {
        val expected = Instant.fromEpochSeconds(1_731_089_200)
        assertEquals(expected, InstantParser.parse(1_731_089_200L))
        assertEquals(expected, InstantParser.parse(1_731_089_200_000L))
    }

    @Test
    fun parse_isoAndDateOnlyStrings() {
        assertEquals(Instant.parse("2026-10-27T10:15:30Z"), InstantParser.parse("2026-10-27T10:15:30Z"))
        assertEquals(Instant.parse("2026-10-27T00:00:00Z"), InstantParser.parse("2026-10-27"))
    }

    @Test
    fun parse_nullBlankAndMalformedResolveToDistantPast() {
        assertEquals(Instant.DISTANT_PAST, InstantParser.parse(null))
        assertEquals(Instant.DISTANT_PAST, InstantParser.parse("   "))
        assertEquals(Instant.DISTANT_PAST, InstantParser.parse("Timestamp(seconds=1, nanoseconds=0)"))
        assertEquals(Instant.DISTANT_PAST, InstantParser.parse(Any()))
    }

    @Test
    fun parse_customDefault() {
        val fallback = Instant.fromEpochSeconds(42)
        assertEquals(fallback, InstantParser.parse(null, fallback))
    }
}
