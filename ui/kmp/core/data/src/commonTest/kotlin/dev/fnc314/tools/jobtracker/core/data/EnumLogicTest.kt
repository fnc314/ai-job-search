package dev.fnc314.tools.jobtracker.core.data

import dev.fnc314.tools.jobtracker.core.data.database.DatabaseConverters
import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.CompPeriod
import dev.fnc314.tools.jobtracker.core.data.model.CompVsTarget
import dev.fnc314.tools.jobtracker.core.data.model.Confidence
import dev.fnc314.tools.jobtracker.core.data.model.DerivedFrom
import dev.fnc314.tools.jobtracker.core.data.model.EventActor
import dev.fnc314.tools.jobtracker.core.data.model.Region
import dev.fnc314.tools.jobtracker.core.data.model.SyncKind
import dev.fnc314.tools.jobtracker.core.data.model.SyncState
import dev.fnc314.tools.jobtracker.core.data.model.Tag
import dev.fnc314.tools.jobtracker.core.data.model.TrackerArtifactKind
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEditKind
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEditState
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEventType
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.model.Trigger
import dev.fnc314.tools.jobtracker.core.data.model.WorkMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EnumLogicTest {

    // --- TrackerStatus Tests ---

    @Test
    fun testTrackerStatus_fromFirestoreVariations() {
        assertEquals(TrackerStatus.LEAD, TrackerStatus.fromFirestore("status:lead"))
        assertEquals(TrackerStatus.APPLIED, TrackerStatus.fromFirestore("status:applied"))
        assertEquals(TrackerStatus.APPLIED, TrackerStatus.fromFirestore("APPLIED"))
        assertEquals(TrackerStatus.APPLIED, TrackerStatus.fromFirestore("applied"))
        assertEquals(TrackerStatus.INTERVIEW, TrackerStatus.fromFirestore("status:interview"))
        assertEquals(TrackerStatus.OFFER_EXPECTED, TrackerStatus.fromFirestore("status:offer"))
    }

    @Test
    fun testTrackerStatus_fromFirestoreBlankAndUnrecognized() {
        assertEquals(TrackerStatus.UNSPECIFIED, TrackerStatus.fromFirestore(null))
        assertEquals(TrackerStatus.UNSPECIFIED, TrackerStatus.fromFirestore(""))
        assertEquals(TrackerStatus.UNSPECIFIED, TrackerStatus.fromFirestore("   "))

        val unrecognized = TrackerStatus.fromFirestore("  status:unknown-gibberish\t ")
        assertIs<TrackerStatus.Unrecognized>(unrecognized)
        assertEquals("  status:unknown-gibberish\t ", unrecognized.wireKey)
        assertEquals("  status:unknown-gibberish\t ", unrecognized.toFirestore())
    }

    @Test
    fun testTrackerStatus_ActiveVsTerminalClassification() {
        // Active pipeline stages
        assertTrue(TrackerStatus.LEAD.isActive)
        assertTrue(TrackerStatus.DRAFTING.isActive)
        assertTrue(TrackerStatus.APPLIED.isActive)
        assertTrue(TrackerStatus.INTERVIEW.isActive)
        assertTrue(TrackerStatus.OFFER_EXPECTED.isActive)

        // Non-active stages
        assertFalse(TrackerStatus.NONE.isActive)
        assertFalse(TrackerStatus.UNSPECIFIED.isActive)
        assertFalse(TrackerStatus.HIRED.isActive)
        assertFalse(TrackerStatus.REJECTED.isActive)
        assertFalse(TrackerStatus.Unrecognized("custom").isActive)

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
        assertFalse(TrackerStatus.Unrecognized("custom").isTerminal)
    }

    @Test
    fun testTrackerStatus_DisplayNameAndFirestoreConsistency() {
        TrackerStatus.entries.forEach { status ->
            assertTrue(status.displayName().isNotBlank(), "DisplayName must not be blank for ${status.wireKey}")
            assertEquals(status.displayName, status.displayName())
            assertTrue(status.toFirestore().startsWith("status:"), "Firestore value must start with status: for ${status.wireKey}")
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
        assertEquals(ApplicationChannel.OUTREACH_LINKEDIN, ApplicationChannel.fromFirestore("channel:outreach:linkedin"))
    }

    @Test
    fun testApplicationChannel_fromFirestoreBlankAndUnrecognized() {
        assertEquals(ApplicationChannel.UNSPECIFIED, ApplicationChannel.fromFirestore(null))
        assertEquals(ApplicationChannel.UNSPECIFIED, ApplicationChannel.fromFirestore(""))
        val unknown = ApplicationChannel.fromFirestore(" random_unsupported_portal ")
        assertIs<ApplicationChannel.Unrecognized>(unknown)
        assertEquals(" random_unsupported_portal ", unknown.wireKey)
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
    fun testWorkMode_fromFirestoreBlankAndUnrecognized() {
        assertEquals(WorkMode.UNSPECIFIED, WorkMode.fromFirestore(null))
        assertEquals(WorkMode.UNSPECIFIED, WorkMode.fromFirestore("   "))
        val unknown = WorkMode.fromFirestore("telepathic\n")
        assertIs<WorkMode.Unrecognized>(unknown)
        assertEquals("telepathic\n", unknown.wireKey)
    }

    @Test
    fun testWorkMode_DisplayNameCoverage() {
        WorkMode.entries.forEach { mode ->
            assertTrue(mode.displayName().isNotBlank(), "DisplayName must not be blank for ${mode.wireKey}")
            assertTrue(mode.toFirestore().startsWith("work-mode:"), "Firestore value must start with work-mode: for ${mode.wireKey}")
        }
    }

    // --- Remaining Sealed WireContract Types & Room Converters ---

    @Test
    fun testAllRemainingWireContracts_KnownAndUnrecognized() {
        assertEquals(Confidence.HIGH, Confidence.fromWireKey("high"))
        assertEquals(Confidence.HIGH, Confidence.fromWireKey("confidence:high"))
        assertIs<Confidence.Unrecognized>(Confidence.fromWireKey("ultra"))

        assertEquals(DerivedFrom.EMAIL, DerivedFrom.fromWireKey("gmail"))
        assertEquals(DerivedFrom.EMAIL, DerivedFrom.fromWireKey("email"))
        assertEquals(DerivedFrom.EMAIL, DerivedFrom.fromWireKey("derived:email"))
        assertIs<DerivedFrom.Unrecognized>(DerivedFrom.fromWireKey(" psychic "))

        assertEquals(EventActor.EMAIL_SYNC, EventActor.fromWireKey("actor:gmail-sync"))
        assertEquals(EventActor.EMAIL_SYNC, EventActor.fromWireKey("actor:email-sync"))
        assertIs<EventActor.Unrecognized>(EventActor.fromWireKey("actor:alien"))

        assertEquals(TrackerArtifactKind.CV, TrackerArtifactKind.fromWireKey("artifact:cv"))
        assertEquals(TrackerEditKind.EMAIL_SYNC, TrackerEditKind.fromWireKey("proposal:gmail-sync"))
        assertEquals(TrackerEditKind.EMAIL_SYNC, TrackerEditKind.fromWireKey("proposal:email-sync"))
        assertEquals(TrackerEditState.PENDING, TrackerEditState.fromWireKey("proposal:pending"))
        assertEquals(TrackerEventType.APPLY, TrackerEventType.fromWireKey("event:applied"))
        assertEquals(TrackerEventType.RECRUITER_OUTREACH, TrackerEventType.fromWireKey("event:recruiter-outreach"))
        assertEquals(Trigger.CRON, Trigger.fromWireKey("trigger:cron"))
        assertEquals(SyncKind.Known.EMAIL_COLLECT, SyncKind.fromWireKey("sync:gmail-collect"))
        assertEquals(SyncKind.Known.EMAIL_COLLECT, SyncKind.fromWireKey("sync:email-collect"))
        assertEquals(SyncState.Known.PENDING, SyncState.fromWireKey("sync:pending"))
        assertEquals(Region.Known.PIT, Region.fromWireKey("PIT"))
        assertEquals(Tag.Known.FIT_STRONG, Tag.fromWireKey("fit:strong"))
        assertEquals(CompPeriod.Known.YEAR, CompPeriod.fromWireKey("year"))
        assertEquals(CompVsTarget.Known.ABOVE, CompVsTarget.fromWireKey("above"))
    }

    @Test
    fun testDatabaseConverters_RoundTripAndLegacyNameSupport() {
        val converters = DatabaseConverters()

        // Canonical wireKey round-trip
        val serializedStatus = converters.fromTrackerStatus(TrackerStatus.INTERVIEW)
        assertEquals("status:interview", serializedStatus)
        assertEquals(TrackerStatus.INTERVIEW, converters.toTrackerStatus(serializedStatus))

        // Legacy enum .name support
        assertEquals(TrackerStatus.INTERVIEW, converters.toTrackerStatus("INTERVIEW"))
        assertEquals(ApplicationChannel.GREENHOUSE, converters.toApplicationChannel("GREENHOUSE"))

        // Unrecognized preservation through Room converter
        val customStatus = TrackerStatus.fromWireKey("status:custom-stage")
        val storedCustom = converters.fromTrackerStatus(customStatus)
        assertEquals("status:custom-stage", storedCustom)
        assertEquals(customStatus, converters.toTrackerStatus(storedCustom))
    }
}
