package dev.fnc314.tools.jobsearch.records.parser

import dev.fnc314.tools.jobsearch.parser.ContractError
import dev.fnc314.tools.jobsearch.parser.DocumentIdGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DocumentIdGeneratorTest {

    @Test
    fun testSlugIsAsciiKebab() {
        assertEquals("doordash", DocumentIdGenerator.slug("DoorDash"))
        assertEquals("ppg-industries", DocumentIdGenerator.slug("PPG Industries"))
        assertEquals("at-t", DocumentIdGenerator.slug("AT&T"))
        assertEquals("washington-post", DocumentIdGenerator.slug("The Washington Post"))
        assertEquals("trade-desk", DocumentIdGenerator.slug("The Trade Desk"))
        assertEquals("scout-motors", DocumentIdGenerator.slug("Scout Motors Inc."))
        assertEquals("major-league-baseball", DocumentIdGenerator.slug("Major League Baseball (MLB)"))
        assertEquals("snap", DocumentIdGenerator.slug("Snap Inc."))
        assertEquals("ford", DocumentIdGenerator.slug("Ford Motor Company"))
        assertEquals("ford", DocumentIdGenerator.slug("Ford"))
        assertEquals("meta", DocumentIdGenerator.slug("Meta"))
        assertEquals("meta", DocumentIdGenerator.slug("Meta Platforms"))
        assertEquals("meta", DocumentIdGenerator.slug("Meta Platforms, Inc."))
    }

    @Test
    fun testNormalizeDirNameLowerKebab() {
        assertEquals("washington-post", DocumentIdGenerator.normalizeDirName("The Washington Post"))
        assertEquals("senior-software-engineer-android", DocumentIdGenerator.normalizeDirName("Sr. Software Engineer, Android"))
        assertEquals("scout-motors", DocumentIdGenerator.normalizeDirName("Scout Motors Inc."))
        assertEquals("major-league-baseball", DocumentIdGenerator.normalizeDirName("Major League Baseball (MLB)"))
        assertEquals("ebay", DocumentIdGenerator.normalizeDirName("EBay"))
    }

    @Test
    fun testSlugDoesNotEmitLeadingOrTrailingHyphens() {
        assertEquals("acme", DocumentIdGenerator.slug("  Acme  "))
        assertEquals("acme", DocumentIdGenerator.slug("!!!Acme!!!"))
        for (value in listOf("Acme", "  Acme  ", "-(Acme)-", "A  B")) {
            val produced = DocumentIdGenerator.slug(value)
            assertFalse(produced.startsWith("-"), produced)
            assertFalse(produced.endsWith("-"), produced)
        }
    }

    @Test
    fun testSlugStripsAccentsRatherThanDroppingLetters() {
        assertEquals("zurich", DocumentIdGenerator.slug("Zürich"))
    }

    @Test
    fun testSlugFallsBackWhenEverythingIsStripped() {
        assertEquals("unknown", DocumentIdGenerator.slug("!!!", fallback = "unknown"))
        assertEquals("", DocumentIdGenerator.slug(""))
    }

    @Test
    fun testDocumentIdMatchesTheHistoricalScheme() {
        assertEquals(
            "doordash--software-engineer-devex",
            DocumentIdGenerator.documentId("DoorDash", "Software Engineer, DevEx"),
        )
    }

    @Test
    fun testDocumentIdWithoutARoleIsTheBareCompany() {
        assertEquals("acme", DocumentIdGenerator.documentId("Acme", null))
        assertEquals("acme", DocumentIdGenerator.documentId("Acme", ""))
    }

    @Test
    fun testDocumentIdRequiresACompany() {
        try {
            DocumentIdGenerator.documentId("", "Engineer")
            assertTrue(false, "should have thrown ContractError")
        } catch (expected: ContractError) {
            assertTrue(expected.message.orEmpty().isNotEmpty())
        }
    }
}
