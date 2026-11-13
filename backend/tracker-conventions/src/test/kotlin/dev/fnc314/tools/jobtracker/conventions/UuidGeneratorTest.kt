package dev.fnc314.tools.jobtracker.conventions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UuidGeneratorTest {

    @Test
    fun testNewUuidIsValid() {
        val uuid = UuidGenerator.newUuid()
        assertTrue(UuidGenerator.isUuid(uuid), "Generated UUID should be valid: $uuid")
    }

    @Test
    fun testNewUuidIsUnique() {
        val uuid1 = UuidGenerator.newUuid()
        val uuid2 = UuidGenerator.newUuid()
        assertTrue(uuid1 != uuid2, "Generated UUIDs should be unique")
    }

    @Test
    fun testIsUuidValidInput() {
        assertTrue(UuidGenerator.isUuid("0194f5c7-8f1e-7a2b-9c3d-4e5f6a7b8c9d"))
    }

    @Test
    fun testIsUuidInvalidInput() {
        assertFalse(UuidGenerator.isUuid(""))
        assertFalse(UuidGenerator.isUuid("not-a-uuid"))
        assertFalse(UuidGenerator.isUuid("0194f5c7-8f1e-4a2b-9c3d-4e5f6a7b8c9d")) // v4, not v7
    }

    @Test
    fun testUuidTimestampExtraction() {
        val uuid = UuidGenerator.newUuid()
        val timestamp = UuidGenerator.uuidTimestamp(uuid)
        val now = System.currentTimeMillis()
        val diff = kotlin.math.abs(now - timestamp)
        assertTrue(diff < 5000, "Timestamp should be within 5 seconds of current time: $timestamp vs $now")
    }

    @Test
    fun testUuidTimestampInvalidInput() {
        try {
            UuidGenerator.uuidTimestamp("not-a-uuid")
            assertTrue(false, "Should have thrown ContractError")
        } catch (expected: ContractError) {
            assertTrue(expected.message.orEmpty().contains("not a UUIDv7"))
        }
    }
}
