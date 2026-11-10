package dev.fnc314.tools.jobsearch.records.parser

import dev.fnc314.tools.jobsearch.parser.CompanyMetricsParser
import dev.fnc314.tools.jobsearch.tracker.v1.ApplicationChannel
import dev.fnc314.tools.jobsearch.tracker.v1.CompanyMetrics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompanyMetricsParserTest {

    @Test
    fun `test fromMap and toMap roundtrip serialization`() {
        val inputMap = mapOf(
            "counts" to mapOf(
                "draft" to 2,
                "submitted" to 5,
                "interviewing" to 3,
                "offer" to 1,
                "rejected" to 4,
                "total" to 15
            ),
            "percentages" to mapOf(
                "draft" to 0.13333334f,
                "submitted" to 0.33333334f,
                "interviewing" to 0.2f,
                "offer" to 0.06666667f,
                "rejected" to 0.26666668f
            ),
            "averageFitScore" to 88,
            "salaryRange" to mapOf(
                "min" to 150000.0,
                "max" to 210000.0,
                "currency" to "USD",
                "display" to "$150k - $210k"
            ),
            "primaryChannel" to "LINKEDIN",
            "locations" to listOf("Pittsburgh, PA", "Remote"),
            "lastCalculatedAt" to "2026-11-10"
        )

        val proto: CompanyMetrics = CompanyMetricsParser.fromMap(inputMap)
        assertEquals(2, proto.counts.draft)
        assertEquals(5, proto.counts.submitted)
        assertEquals(3, proto.counts.interviewing)
        assertEquals(1, proto.counts.offer)
        assertEquals(4, proto.counts.rejected)
        assertEquals(15, proto.counts.total)
        assertEquals(88, proto.averageFitScore)
        assertEquals(150000.0, proto.salaryRange.min)
        assertEquals(210000.0, proto.salaryRange.max)
        assertEquals("USD", proto.salaryRange.currency)
        assertEquals("$150k - $210k", proto.salaryRange.display)
        assertEquals(ApplicationChannel.LINKEDIN, proto.primaryChannel)
        assertEquals(listOf("Pittsburgh, PA", "Remote"), proto.locationsList)

        val outputMap = CompanyMetricsParser.toMap(proto)
        assertEquals(inputMap["counts"], outputMap["counts"])
        assertEquals(inputMap["percentages"], outputMap["percentages"])
        assertEquals(88, outputMap["averageFitScore"])
        assertEquals("LINKEDIN", outputMap["primaryChannel"])
        assertEquals(listOf("Pittsburgh, PA", "Remote"), outputMap["locations"])
        assertEquals("2026-11-10", outputMap["lastCalculatedAt"])
    }
}
