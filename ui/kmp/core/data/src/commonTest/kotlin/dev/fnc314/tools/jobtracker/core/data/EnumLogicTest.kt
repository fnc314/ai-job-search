package dev.fnc314.tools.jobtracker.core.data

import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.model.WorkMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnumLogicTest {

    // --- TrackerStatus Tests ---

    @Test
    fun testTrackerStatus_fromFirestoreVariations() {
        assertEquals(TrackerStatus.APPLIED, TrackerStatus.fromFirestore("status:applied"))
        assertEquals(TrackerStatus.APPLIED, TrackerStatus.fromFirestore("APPLIED"))
        assertEquals(TrackerStatus.APPLIED, TrackerStatus.fromFirestore("applied"))
        assertEquals(TrackerStatus.INTERVIEW, TrackerStatus.fromFirestore("status:interview"))
        assertEquals(TrackerStatus.OFFER_EXPECTED, TrackerStatus.fromFirestore("status:offer"))
    }

    @Test
    fun testTrackerStatus_fromFirestoreUnknownAndBlankFallback() {
        assertEquals(TrackerStatus.UNSPECIFIED, TrackerStatus.fromFirestore(null))
        assertEquals(TrackerStatus.UNSPECIFIED, TrackerStatus.fromFirestore(""))
        assertEquals(TrackerStatus.UNSPECIFIED, TrackerStatus.fromFirestore("   "))
        assertEquals(TrackerStatus.UNSPECIFIED, TrackerStatus.fromFirestore("status:unknown-gibberish"))
    }

    @Test
    fun testTrackerStatus_ActiveVsTerminalClassification() {
        // Active pipeline stages
        assertTrue(TrackerStatus.DRAFTING.isActive)
        assertTrue(TrackerStatus.APPLIED.isActive)
        assertTrue(TrackerStatus.INTERVIEW.isActive)
        assertTrue(TrackerStatus.OFFER_EXPECTED.isActive)

        // Non-active stages
        assertFalse(TrackerStatus.NONE.isActive)
        assertFalse(TrackerStatus.UNSPECIFIED.isActive)
        assertFalse(TrackerStatus.HIRED.isActive)
        assertFalse(TrackerStatus.REJECTED.isActive)

        // Terminal stages
        assertTrue(TrackerStatus.HIRED.isTerminal)
        assertTrue(TrackerStatus.REJECTED.isTerminal)
        assertTrue(TrackerStatus.NO_RESPONSE.isTerminal)
        assertTrue(TrackerStatus.OFFER_DECLINED.isTerminal)
        assertTrue(TrackerStatus.APPLICATION_WITHDRAWN.isTerminal)

        // Non-terminal stages
        assertFalse(TrackerStatus.APPLIED.isTerminal)
        assertFalse(TrackerStatus.INTERVIEW.isTerminal)
        assertFalse(TrackerStatus.DRAFTING.isTerminal)
    }

    @Test
    fun testTrackerStatus_DisplayNameCoverage() {
        TrackerStatus.entries.forEach { status ->
            assertTrue(status.displayName().isNotBlank(), "DisplayName must not be blank for ${status.name}")
            assertEquals(status.displayName, status.displayName())
        }
    }

    @Test
    fun testTrackerStatus_FirestoreValueConsistency() {
        TrackerStatus.entries.forEach { status ->
            assertTrue(status.toFirestore().startsWith("status:"), "Firestore value must start with status: for ${status.name}")
        }
    }

    // --- ApplicationChannel & WorkMode Tests ---

    @Test
    fun testApplicationChannel_fromFirestoreAliases() {
        assertEquals(ApplicationChannel.LINKEDIN, ApplicationChannel.fromFirestore("channel:linkedin"))
        assertEquals(ApplicationChannel.LINKEDIN, ApplicationChannel.fromFirestore("linkedin"))
        assertEquals(ApplicationChannel.LINKEDIN, ApplicationChannel.fromFirestore("LINKEDIN"))
        assertEquals(ApplicationChannel.REFERRAL, ApplicationChannel.fromFirestore("referral"))
        assertEquals(ApplicationChannel.GREENHOUSE, ApplicationChannel.fromFirestore("channel:greenhouse"))
    }

    @Test
    fun testApplicationChannel_fromFirestoreUnknownFallback() {
        assertEquals(ApplicationChannel.UNSPECIFIED, ApplicationChannel.fromFirestore(null))
        assertEquals(ApplicationChannel.UNSPECIFIED, ApplicationChannel.fromFirestore(""))
        assertEquals(ApplicationChannel.UNSPECIFIED, ApplicationChannel.fromFirestore("random_unsupported_portal"))
    }

    @Test
    fun testWorkMode_fromFirestoreVariations() {
        assertEquals(WorkMode.REMOTE, WorkMode.fromFirestore("work-mode:remote"))
        assertEquals(WorkMode.REMOTE, WorkMode.fromFirestore("remote"))
        assertEquals(WorkMode.REMOTE, WorkMode.fromFirestore("REMOTE"))
        assertEquals(WorkMode.HYBRID, WorkMode.fromFirestore("work-mode:hybrid"))
        assertEquals(WorkMode.ONSITE, WorkMode.fromFirestore("work-mode:onsite"))
    }

    @Test
    fun testWorkMode_fromFirestoreBlankFallback() {
        assertEquals(WorkMode.UNSPECIFIED, WorkMode.fromFirestore(null))
        assertEquals(WorkMode.UNSPECIFIED, WorkMode.fromFirestore("   "))
        assertEquals(WorkMode.UNSPECIFIED, WorkMode.fromFirestore("telepathic"))
    }

    @Test
    fun testWorkMode_DisplayNameCoverage() {
        WorkMode.entries.forEach { mode ->
            assertTrue(mode.displayName().isNotBlank(), "DisplayName must not be blank for ${mode.name}")
            assertTrue(mode.toFirestore().startsWith("work-mode:"), "Firestore value must start with work-mode: for ${mode.name}")
        }
    }
}
