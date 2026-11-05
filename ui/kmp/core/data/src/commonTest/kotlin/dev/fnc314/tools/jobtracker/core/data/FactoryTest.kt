package dev.fnc314.tools.jobtracker.core.data

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.Confidence
import dev.fnc314.tools.jobtracker.core.data.model.DerivedFrom
import dev.fnc314.tools.jobtracker.core.data.model.EventActor
import dev.fnc314.tools.jobtracker.core.data.model.TrackerArtifactKind
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEventType
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FactoryTest {

    @Test
    fun testFactory_FitRatingAutoSynthesizesFitEvaluation() {
        val app = Factory.createJobApplication(
            id = "app-auto-fit",
            company = "Synthesized Corp",
            fitRating = 85,
            fitEvaluation = null,
        )
        assertEquals(85, app.fitRating)
        assertNotNull(app.fitEvaluation)
        assertEquals(85, app.fitEvaluation.score)
    }

    @Test
    fun testFactory_ConflictPreservesExplicitFitEvaluation() {
        val customFit = Factory.createTrackerFit(score = 90)
        val app = Factory.createJobApplication(
            id = "app-conflict",
            company = "Divergent Corp",
            fitRating = 60,
            fitEvaluation = customFit,
        )
        // fitRating is 60, but the custom TrackerFit evaluation instance is preserved
        assertEquals(60, app.fitRating)
        assertEquals(customFit, app.fitEvaluation)
        assertEquals(90, app.fitEvaluation?.score)
    }

    @Test
    fun testFactory_ListDefensiveCopying() {
        val mutableTags = ArrayList<String>()
        mutableTags.add("android")
        mutableTags.add("kmp")

        val mutableContacts = ArrayList<String>()
        mutableContacts.add("recruiter@test.com")

        val app = Factory.createJobApplication(
            id = "app-defensive-copy",
            company = "Fortress Corp",
            tags = mutableTags,
            contactIds = mutableContacts,
        )

        // Mutating the original input lists after creation
        mutableTags.clear()
        mutableContacts.add("leak@test.com")

        assertEquals(2, app.tags.size)
        assertEquals(listOf("android", "kmp"), app.tags)
        assertEquals(1, app.contactIds.size)
        assertEquals(listOf("recruiter@test.com"), app.contactIds)
    }

    @Test
    fun testFactory_TrackerEventDefaults() {
        val event = Factory.createTrackerEvent(
            id = "evt-1",
            applicationId = "app-1",
            type = TrackerEventType.CORRECT_STATUS,
            statusFrom = TrackerStatus.DRAFTING,
            statusTo = TrackerStatus.APPLIED,
        )
        assertEquals("evt-1", event.id)
        assertEquals("app-1", event.applicationId)
        assertEquals(EventActor.USER, event.actor)
        assertEquals(DerivedFrom.MANUAL, event.derivedFrom)
        assertEquals(Confidence.HIGH, event.confidence)
        assertNotNull(event.createdAt)
        assertNull(event.note)
    }

    @Test
    fun testFactory_TrackerArtifactSpecialCharsAndHashes() {
        val artifact = Factory.createTrackerArtifact(
            id = "art-1",
            applicationId = "app-1",
            kind = TrackerArtifactKind.CV,
            filename = "CV_Franco_Colaizzi_2026_日本語.pdf",
            contentType = "application/pdf",
            size = 142857L,
            sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
        )
        assertEquals("CV_Franco_Colaizzi_2026_日本語.pdf", artifact.filename)
        assertEquals(142857L, artifact.size)
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", artifact.sha256)
        assertNotNull(artifact.createdAt)
    }
}
