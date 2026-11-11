package dev.fnc314.tools.jobsearch.records.parser

import dev.fnc314.tools.jobsearch.parser.ProposalClassifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProposalClassifierTest {

    // === Classification ===

    @Test
    fun testClassifyRejection() {
        val result = ProposalClassifier.classify(
            "Application Status Update",
            "Unfortunately, we have decided not to move forward with your application."
        )
        assertNotNull(result)
        assertEquals("REJECT", result.eventType)
        assertEquals("REJECTED", result.statusTo)
        assertEquals("HIGH_CONFIDENCE", result.confidence)
    }

    @Test
    fun testClassifyRejectionTakesPrecedence() {
        // A rejection that also says "thank you for applying" should be REJECT
        val result = ProposalClassifier.classify(
            "Thank you for applying - unfortunately...",
            "Thank you for applying. Unfortunately, we will not be moving forward."
        )
        assertNotNull(result)
        assertEquals("REJECT", result.eventType)
    }

    @Test
    fun testClassifyOffer() {
        val result = ProposalClassifier.classify(
            "Job Offer - Senior Android Engineer",
            "We are pleased to offer you the position of Senior Android Engineer."
        )
        assertNotNull(result)
        assertEquals("OFFER", result.eventType)
        assertEquals("OFFER_EXPECTED", result.statusTo)
    }

    @Test
    fun testClassifyInterview() {
        val result = ProposalClassifier.classify(
            "Interview Request",
            "We would like to schedule a phone screen to discuss your qualifications."
        )
        assertNotNull(result)
        assertEquals("SCHEDULE_INTERVIEW", result.eventType)
        assertEquals("INTERVIEW", result.statusTo)
    }

    @Test
    fun testClassifyAssessment() {
        val result = ProposalClassifier.classify(
            "Next Steps: Coding Assessment",
            "As part of our process, we would like you to complete an online assessment."
        )
        assertNotNull(result)
        assertEquals("ASSESS", result.eventType)
        assertNull(result.statusTo)
    }

    @Test
    fun testClassifyAcknowledgement() {
        val result = ProposalClassifier.classify(
            "Application Received",
            "We've received your application and will review it shortly."
        )
        assertNotNull(result)
        assertEquals("ACKNOWLEDGE", result.eventType)
        assertNull(result.statusTo)
    }

    @Test
    fun testClassifyRecruiterOutreach() {
        val result = ProposalClassifier.classify(
            "Exciting opportunity at TechCorp",
            "I came across your profile and thought you might be interested in a role we have."
        )
        assertNotNull(result)
        assertEquals("RECRUITER_OUTREACH", result.eventType)
        assertNull(result.statusTo)
    }

    @Test
    fun testClassifyNoMatch() {
        val result = ProposalClassifier.classify(
            "Weekly Newsletter",
            "Here are the top stories this week in technology."
        )
        assertNull(result)
    }

    @Test
    fun testClassifyEmptyInput() {
        val result = ProposalClassifier.classify(null, null)
        assertNull(result)
    }

    // === Company Matching ===

    @Test
    fun testCompanyMatchesExactName() {
        val app = mapOf("company" to "Capital One")
        val text = "Capital One is reaching out regarding your application"
        assertTrue(ProposalClassifier.companyMatches(text, app))
    }

    @Test
    fun testCompanyMatchesCaseInsensitive() {
        val app = mapOf("company" to "Capital One")
        val text = "capital one application update"
        assertTrue(ProposalClassifier.companyMatches(text, app))
    }

    @Test
    fun testCompanyMatchesSlugWords() {
        val app = mapOf("company" to "PatientPoint")
        val text = "PatientPoint engineering team update"
        assertTrue(ProposalClassifier.companyMatches(text, app))
    }

    @Test
    fun testCompanyNoMatch() {
        val app = mapOf("company" to "Capital One")
        val text = "Google is reaching out regarding your application"
        assertFalse(ProposalClassifier.companyMatches(text, app))
    }

    @Test
    fun testCompanyEmptyName() {
        val app = mapOf("company" to "")
        val text = "Some company update"
        assertFalse(ProposalClassifier.companyMatches(text, app))
    }

    // === Message Matching ===

    @Test
    fun testMatchExplicitApplicationId() {
        val apps = mapOf("capital-one--android" to mapOf("company" to "Capital One"))
        val message = mapOf("applicationId" to "capital-one--android")
        val result = ProposalClassifier.match(message, apps)
        assertEquals("capital-one--android", result.applicationId)
        assertNull(result.reason)
    }

    @Test
    fun testMatchExplicitIdNotFound() {
        val apps = mapOf("capital-one--android" to mapOf("company" to "Capital One"))
        val message = mapOf("applicationId" to "nonexistent")
        val result = ProposalClassifier.match(message, apps)
        assertNull(result.applicationId)
        assertNotNull(result.reason)
        assertTrue(result.reason.contains("does not exist"))
    }

    @Test
    fun testMatchByCompany() {
        val apps = mapOf("capital-one--android" to mapOf("company" to "Capital One"))
        val message = mapOf(
            "subject" to "Application Update",
            "sender" to "noreply@capitalone.com",
            "snippet" to "Capital One application status"
        )
        val result = ProposalClassifier.match(message, apps)
        assertEquals("capital-one--android", result.applicationId)
    }

    @Test
    fun testMatchNoCompany() {
        val apps = mapOf("capital-one--android" to mapOf("company" to "Capital One"))
        val message = mapOf(
            "subject" to "Newsletter",
            "sender" to "news@example.com",
            "snippet" to "Weekly tech digest"
        )
        val result = ProposalClassifier.match(message, apps)
        assertNull(result.applicationId)
        assertNotNull(result.reason)
        assertTrue(result.reason.contains("no application matched"))
    }

    @Test
    fun testMatchAmbiguous() {
        val apps = mapOf(
            "capital-one--android" to mapOf("company" to "Capital One"),
            "capital-one--ios" to mapOf("company" to "Capital One"),
        )
        val message = mapOf(
            "subject" to "Update",
            "sender" to "Capital One",
            "snippet" to "Capital One update"
        )
        val result = ProposalClassifier.match(message, apps)
        assertNull(result.applicationId)
        assertNotNull(result.reason)
        assertTrue(result.reason.contains("ambiguous"))
    }

    // === Company Extraction ===

    @Test
    fun testExtractCompanyFromSender() {
        val message = mapOf(
            "sender" to "TechCorp Recruiting <jobs@techcorp.com>",
            "subject" to "Exciting opportunity"
        )
        val company = ProposalClassifier.extractCompany(message)
        assertNotNull(company)
        assertTrue(company.isNotEmpty())
    }

    @Test
    fun testExtractCompanyNoMatch() {
        val message = mapOf(
            "sender" to "noreply@example.com",
            "subject" to "Weekly Newsletter"
        )
        val company = ProposalClassifier.extractCompany(message)
        assertNull(company)
    }

    // === Classifier Rules ===

    @Test
    fun testClassifiersNotEmpty() {
        assertTrue(ProposalClassifier.CLASSIFIERS.isNotEmpty())
    }

    @Test
    fun testRejectClassifierFirst() {
        // REJECT should be the first classifier (highest priority)
        assertEquals("REJECT", ProposalClassifier.CLASSIFIERS[0].eventType)
    }
}
