package dev.fnc314.aijobtracker.records.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SalaryBenchmarkEngineTest {

    @Test
    fun normalizeCompanyNameStripsCorporateSuffixes() {
        assertEquals("novo nordisk", SalaryBenchmarkEngine.normalizeCompanyName("Novo Nordisk A/S"))
        assertEquals("orsted", SalaryBenchmarkEngine.normalizeCompanyName("Ørsted A/S"))
        assertEquals("a p moller maersk", SalaryBenchmarkEngine.normalizeCompanyName("A.P. Møller - Mærsk A/S"))
    }

    @Test
    fun searchFindsMatchingCompanyAndCity() {
        val dataset = SalaryDataset(
            companies = listOf(
                CompanySalaryBenchmark(
                    company = "Novo Nordisk A/S",
                    city = "Bagsværd",
                    categories = mapOf("engineering" to SalaryCategoryStats(count = 50, index = 112.5))
                ),
                CompanySalaryBenchmark(
                    company = "Ørsted A/S",
                    city = "Gentofte"
                )
            )
        )

        val results = SalaryBenchmarkEngine.search(dataset, "novo")
        assertEquals(1, results.size)
        assertEquals("Novo Nordisk A/S", results[0].company)

        val orstedResults = SalaryBenchmarkEngine.search(dataset, "Orsted")
        assertEquals(1, orstedResults.size)
        assertEquals("Ørsted A/S", orstedResults[0].company)

        val cityFiltered = SalaryBenchmarkEngine.search(dataset, "novo", city = "Copenhagen")
        assertTrue(cityFiltered.isEmpty())
    }
}
