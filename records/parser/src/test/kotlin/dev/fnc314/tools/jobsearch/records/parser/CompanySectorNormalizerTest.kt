package dev.fnc314.tools.jobsearch.records.parser

import dev.fnc314.tools.jobsearch.parser.CompanySectorNormalizer
import dev.fnc314.tools.jobsearch.tracker.v1.CompanySector
import kotlin.test.Test
import kotlin.test.assertEquals

class CompanySectorNormalizerTest {

    @Test
    fun testKnownCompanySectors() {
        assertEquals(CompanySector.FINTECH, CompanySectorNormalizer.normalize("affirm"))
        assertEquals(CompanySector.FINTECH, CompanySectorNormalizer.normalize("pnc"))
        assertEquals(CompanySector.TECH, CompanySectorNormalizer.normalize("anthropic"))
        assertEquals(CompanySector.HEALTHCARE, CompanySectorNormalizer.normalize("baxter-international"))
        assertEquals(CompanySector.RETAIL, CompanySectorNormalizer.normalize("american-eagle-outfitters"))
        assertEquals(CompanySector.AUTOMOTIVE, CompanySectorNormalizer.normalize("ford-motor-company"))
        assertEquals(CompanySector.MEDIA, CompanySectorNormalizer.normalize("disney"))
        assertEquals(CompanySector.EDUCATION, CompanySectorNormalizer.normalize("carnegie-mellon-university"))
        assertEquals(CompanySector.CONSULTING, CompanySectorNormalizer.normalize("deloitte"))
    }

    @Test
    fun testIndustryStringParsing() {
        assertEquals(CompanySector.FINTECH, CompanySectorNormalizer.normalize(industryOrSector = "Financial Services"))
        assertEquals(CompanySector.HEALTHCARE, CompanySectorNormalizer.normalize(industryOrSector = "Hospital & Health Care"))
        assertEquals(CompanySector.AUTOMOTIVE, CompanySectorNormalizer.normalize(industryOrSector = "Motor Vehicle Manufacturing"))
        assertEquals(CompanySector.MEDIA, CompanySectorNormalizer.normalize(industryOrSector = "Broadcast Media Production and Distribution"))
        assertEquals(CompanySector.TECH, CompanySectorNormalizer.normalize(industryOrSector = "Computer Software"))
    }

    @Test
    fun testTokenHeuristicsFallback() {
        assertEquals(CompanySector.FINTECH, CompanySectorNormalizer.normalize(companyName = "First National Bank"))
        assertEquals(CompanySector.HEALTHCARE, CompanySectorNormalizer.normalize(companyName = "Apex Health Clinic"))
        assertEquals(CompanySector.AUTOMOTIVE, CompanySectorNormalizer.normalize(companyName = "Tesla Motors"))
        assertEquals(CompanySector.MEDIA, CompanySectorNormalizer.normalize(companyName = "Global News Post"))
        assertEquals(CompanySector.TECH, CompanySectorNormalizer.normalize(companyName = "Acme Widgets Cloud"))
    }

    @Test
    fun testLinkedinUrlResolution() {
        assertEquals("https://www.linkedin.com/company/anthropicresearch", CompanySectorNormalizer.resolveLinkedinUrl("anthropic"))
        assertEquals("https://www.linkedin.com/company/affirm", CompanySectorNormalizer.resolveLinkedinUrl("affirm"))
        assertEquals("https://www.linkedin.com/company/pnc", CompanySectorNormalizer.resolveLinkedinUrl("pnc"))
    }

    @Test
    fun testFormatDisplay() {
        assertEquals("FinTech", CompanySectorNormalizer.formatDisplay(CompanySector.FINTECH))
        assertEquals("Technology", CompanySectorNormalizer.formatDisplay(CompanySector.TECH))
        assertEquals("Healthcare", CompanySectorNormalizer.formatDisplay(CompanySector.HEALTHCARE))
    }
}
