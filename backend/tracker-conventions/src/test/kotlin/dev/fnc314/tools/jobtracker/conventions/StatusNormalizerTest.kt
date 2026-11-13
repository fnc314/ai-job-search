package dev.fnc314.tools.jobtracker.conventions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StatusNormalizerTest {

    @Test
    fun testNormalizeStatusCanonicalInput() {
        assertEquals("INTERVIEWING", StatusNormalizer.normalizeStatus("INTERVIEWING"))
        assertEquals("HIRED", StatusNormalizer.normalizeStatus("HIRED"))
        assertEquals("REJECTED", StatusNormalizer.normalizeStatus("REJECTED"))
    }

    @Test
    fun testNormalizeStatusLegacyInput() {
        assertEquals("DRAFTED", StatusNormalizer.normalizeStatus("draft"))
        assertEquals("DRAFTED", StatusNormalizer.normalizeStatus("drafting"))
        assertEquals("SUBMITTED", StatusNormalizer.normalizeStatus("applied"))
        assertEquals("RECRUITER_SCREENED", StatusNormalizer.normalizeStatus("screening"))
        assertEquals("INTERVIEWING", StatusNormalizer.normalizeStatus("interview"))
        assertEquals("OFFER", StatusNormalizer.normalizeStatus("offer"))
        assertEquals("HIRED", StatusNormalizer.normalizeStatus("hired"))
        assertEquals("REJECTED", StatusNormalizer.normalizeStatus("rejected"))
        assertEquals("NO_RESPONSE", StatusNormalizer.normalizeStatus("no-response"))
        assertEquals("OFFER_DECLINED", StatusNormalizer.normalizeStatus("offer-declined"))
        assertEquals("APPLICATION_WITHDRAWN", StatusNormalizer.normalizeStatus("withdrawn"))
    }

    @Test
    fun testNormalizeStatusUnknownDefaultsToSubmitted() {
        assertEquals("SUBMITTED", StatusNormalizer.normalizeStatus("unknown_status"))
    }

    @Test
    fun testStatusRankOrdering() {
        assertEquals(0, StatusNormalizer.statusRank["NO_RESPONSE"])
        assertEquals(3, StatusNormalizer.statusRank["REJECTED"])
        assertEquals(4, StatusNormalizer.statusRank["DRAFTED"])
        assertEquals(5, StatusNormalizer.statusRank["SUBMITTED"])
        assertEquals(7, StatusNormalizer.statusRank["INTERVIEWING"])
        assertEquals(9, StatusNormalizer.statusRank["OFFER"])
        assertEquals(11, StatusNormalizer.statusRank["HIRED"])
    }

    @Test
    fun testTerminalStatuses() {
        assertTrue(StatusNormalizer.terminalStatuses.contains("NO_RESPONSE"))
        assertTrue(StatusNormalizer.terminalStatuses.contains("REJECTED"))
        assertTrue(StatusNormalizer.terminalStatuses.contains("HIRED"))
        assertTrue(StatusNormalizer.terminalStatuses.contains("OFFER_DECLINED"))
        assertTrue(StatusNormalizer.terminalStatuses.contains("APPLICATION_WITHDRAWN"))
        assertFalse(StatusNormalizer.terminalStatuses.contains("INTERVIEWING"))
        assertFalse(StatusNormalizer.terminalStatuses.contains("SUBMITTED"))
    }

    @Test
    fun testDeriveStatusFields() {
        val hiredFields = StatusNormalizer.deriveStatusFields("HIRED")
        assertEquals(11, hiredFields["statusRank"])
        assertFalse(hiredFields["isOpen"] as Boolean)

        val interviewingFields = StatusNormalizer.deriveStatusFields("INTERVIEWING")
        assertEquals(7, interviewingFields["statusRank"])
        assertTrue(interviewingFields["isOpen"] as Boolean)
    }
}
