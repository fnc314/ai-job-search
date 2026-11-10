package dev.fnc314.tools.jobtracker.features.companieslist.domain

import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.CompanyMetrics
import dev.fnc314.tools.jobtracker.features.companieslist.data.repository.CompaniesListRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class CompaniesListDomainTest {

    private val sampleCompanies = listOf(
        Company(
            id = "google",
            name = "Google",
            aliases = listOf("Alphabet"),
            website = "https://google.com",
            industry = "Tech",
            createdAt = null,
            updatedAt = null,
            metrics = CompanyMetrics(
                counts = CompanyMetrics.ApplicationCounts(total = 5, submitted = 2, interview = 1),
                averageFitScore = 90.0,
                locations = listOf("Mountain View, CA"),
            ),
        ),
        Company(
            id = "apple",
            name = "Apple",
            aliases = emptyList(),
            website = "https://apple.com",
            industry = "Consumer Electronics",
            createdAt = null,
            updatedAt = null,
            metrics = CompanyMetrics(
                counts = CompanyMetrics.ApplicationCounts(total = 2, draft = 2),
                averageFitScore = 95.0,
                locations = listOf("Cupertino, CA"),
            ),
        ),
        Company(
            id = "pnc",
            name = "PNC Financial Services",
            aliases = listOf("PNC Bank"),
            website = "https://pnc.com",
            industry = "FinTech",
            createdAt = null,
            updatedAt = null,
            metrics = CompanyMetrics(
                counts = CompanyMetrics.ApplicationCounts(total = 10, offer = 1),
                averageFitScore = 80.0,
                locations = listOf("Pittsburgh, PA"),
            ),
        ),
    )

    @Test
    fun testFilterCompanies() {
        val filter = FilterCompaniesUseCase()

        val techResults = filter(sampleCompanies, "tech")
        assertEquals(2, techResults.size) // Google (Tech) and PNC (FinTech)

        val pittsburghResults = filter(sampleCompanies, "pittsburgh")
        assertEquals(1, pittsburghResults.size)
        assertEquals("pnc", pittsburghResults.first().id)

        val emptyFilter = filter(sampleCompanies, "")
        assertEquals(3, emptyFilter.size)

        val openAppsFilter = filter(sampleCompanies, "", CompanyStatusFilter.OPEN_APPLICATIONS)
        assertEquals(1, openAppsFilter.size)
        assertEquals("google", openAppsFilter.first().id)

        val interviewFilter = filter(sampleCompanies, "", CompanyStatusFilter.ONGOING_INTERVIEW)
        assertEquals(1, interviewFilter.size)
        assertEquals("google", interviewFilter.first().id)

        val offerFilter = filter(sampleCompanies, "", CompanyStatusFilter.OPEN_OFFER)
        assertEquals(1, offerFilter.size)
        assertEquals("pnc", offerFilter.first().id)
    }

    @Test
    fun testSortCompanies() {
        val sort = SortCompaniesUseCase()

        val sortedByName = sort(sampleCompanies, CompanySortOption.NAME_ASC)
        assertEquals(listOf("apple", "google", "pnc"), sortedByName.map { it.id })

        val sortedByTotalDesc = sort(sampleCompanies, CompanySortOption.APPLICATIONS_COUNT_DESC)
        assertEquals(listOf("pnc", "google", "apple"), sortedByTotalDesc.map { it.id })

        val sortedByFitDesc = sort(sampleCompanies, CompanySortOption.FIT_SCORE_DESC)
        assertEquals(listOf("apple", "google", "pnc"), sortedByFitDesc.map { it.id })
    }

    @Test
    fun testLoadAllCompanies() = runTest {
        val mockRepo = object : CompaniesListRepository {
            override suspend fun invoke(forceRefresh: Boolean): List<Company> = sampleCompanies
        }
        val useCase = LoadAllCompaniesUseCase(mockRepo)
        val result = useCase()
        assertEquals(3, result.size)
    }
}
