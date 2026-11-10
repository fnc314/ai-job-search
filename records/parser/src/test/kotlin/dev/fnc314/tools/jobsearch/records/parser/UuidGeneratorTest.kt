package dev.fnc314.tools.jobsearch.records.parser

import dev.fnc314.tools.jobsearch.parser.ContractError
import dev.fnc314.tools.jobsearch.parser.UuidGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UuidGeneratorTest {

    @Test
    fun testNewUuidIsCanonicalUuidV7() {
        val value = UuidGenerator.newUuid()
        val pattern = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")
        assertTrue(pattern.matches(value), "UUID should match v7 pattern: $value")
        assertEquals(36, value.length)
        assertTrue(UuidGenerator.isUuid(value))
    }

    @Test
    fun testUuidsAreUnique() {
        val values = (0 until 500).map { UuidGenerator.newUuid() }.toSet()
        assertEquals(500, values.size)
    }

    @Test
    fun testNonUuidV7IsRejected() {
        val v4 = "018f3c1a-4b2e-4d3f-9a41-2c6b8e5d0f17"
        val badVariant = "018f3c1a-4b2e-7d3f-1a41-2c6b8e5d0f17"
        for (bad in listOf("", "not-a-uuid", v4, badVariant, "01JA5Z3M8Q9V0W1X2Y3Z4A5B6C")) {
            assertFalse(UuidGenerator.isUuid(bad), bad)
        }
    }

    @Test
    fun testUuidTimestampThrowsOnInvalid() {
        for (bad in listOf("", "not-a-uuid", "018f3c1a-4b2e-4d3f-9a41-2c6b8e5d0f17")) {
            try {
                UuidGenerator.uuidTimestamp(bad)
                assertTrue(false, "should have thrown ContractError for: $bad")
            } catch (e: ContractError) {
                // expected
            }
        }
    }

    @Test
    fun testUuidTimestampIsCloseToNow() {
        val value = UuidGenerator.newUuid()
        val millis = UuidGenerator.uuidTimestamp(value)
        val now = System.currentTimeMillis()
        val diff = kotlin.math.abs(now - millis)
        assertTrue(diff < 2000, "UUID timestamp should be within 2s of now, but was $diff ms off")
    }
}
