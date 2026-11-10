package dev.fnc314.tools.jobtracker.features.companieslist.ui

import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.CompanyMetrics
import dev.fnc314.tools.jobtracker.features.companieslist.data.repository.CompaniesListRepository
import dev.fnc314.tools.jobtracker.features.companieslist.domain.CompanySortOption
import dev.fnc314.tools.jobtracker.features.companieslist.domain.CompanyStatusFilter
import dev.fnc314.tools.jobtracker.features.companieslist.domain.FilterCompaniesUseCase
import dev.fnc314.tools.jobtracker.features.companieslist.domain.LoadAllCompaniesUseCase
import dev.fnc314.tools.jobtracker.features.companieslist.domain.SortCompaniesUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class CompaniesListViewModelTest {

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
                counts = CompanyMetrics.ApplicationCounts(total = 5, submitted = 3, interview = 1),
                averageFitScore = 90.0,
            ),
        ),
        Company(
            id = "pnc",
            name = "PNC",
            aliases = emptyList(),
            website = "https://pnc.com",
            industry = "FinTech",
            createdAt = null,
            updatedAt = null,
            metrics = CompanyMetrics(
                counts = CompanyMetrics.ApplicationCounts(total = 10, offer = 1),
                averageFitScore = 80.0,
            ),
        ),
    )

    @Test
    fun testInitialLoad_Success() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)

        val mockRepo = object : CompaniesListRepository {
            override suspend fun invoke(forceRefresh: Boolean): List<Company> = sampleCompanies
        }
        val viewModel = CompaniesListViewModel(
            loadAllCompanies = LoadAllCompaniesUseCase(mockRepo),
            filterCompanies = FilterCompaniesUseCase(),
            sortCompanies = SortCompaniesUseCase(),
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CompaniesListUiState.Content)
        assertEquals(2, state.companies.size)
        assertEquals("Google", state.companies[0].name)

        Dispatchers.resetMain()
    }

    @Test
    fun testSearch_FiltersContent() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)

        val mockRepo = object : CompaniesListRepository {
            override suspend fun invoke(forceRefresh: Boolean): List<Company> = sampleCompanies
        }
        val viewModel = CompaniesListViewModel(
            loadAllCompanies = LoadAllCompaniesUseCase(mockRepo),
            filterCompanies = FilterCompaniesUseCase(),
            sortCompanies = SortCompaniesUseCase(),
        )

        advanceUntilIdle()

        viewModel.onIntent(CompaniesListUiIntent.Search("pnc"))
        advanceUntilIdle()

        val state = viewModel.uiState.value as CompaniesListUiState.Content
        assertEquals(1, state.companies.size)
        assertEquals("pnc", state.companies[0].id)

        Dispatchers.resetMain()
    }

    @Test
    fun testToggleExpand() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)

        val mockRepo = object : CompaniesListRepository {
            override suspend fun invoke(forceRefresh: Boolean): List<Company> = sampleCompanies
        }
        val viewModel = CompaniesListViewModel(
            loadAllCompanies = LoadAllCompaniesUseCase(mockRepo),
            filterCompanies = FilterCompaniesUseCase(),
            sortCompanies = SortCompaniesUseCase(),
        )

        advanceUntilIdle()

        viewModel.onIntent(CompaniesListUiIntent.ToggleExpand("google"))
        advanceUntilIdle()

        val state = viewModel.uiState.value as CompaniesListUiState.Content
        val google = state.companies.first { it.id == "google" }
        assertTrue(google.isExpanded)

        Dispatchers.resetMain()
    }

    @Test
    fun testStatusFilter_FiltersContent() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)

        val mockRepo = object : CompaniesListRepository {
            override suspend fun invoke(forceRefresh: Boolean): List<Company> = sampleCompanies
        }
        val viewModel = CompaniesListViewModel(
            loadAllCompanies = LoadAllCompaniesUseCase(mockRepo),
            filterCompanies = FilterCompaniesUseCase(),
            sortCompanies = SortCompaniesUseCase(),
        )

        advanceUntilIdle()

        viewModel.onIntent(CompaniesListUiIntent.ChangeStatusFilter(CompanyStatusFilter.OPEN_OFFER))
        advanceUntilIdle()

        val state = viewModel.uiState.value as CompaniesListUiState.Content
        assertEquals(1, state.companies.size)
        assertEquals("pnc", state.companies[0].id)
        assertEquals(CompanyStatusFilter.OPEN_OFFER, state.statusFilter)

        Dispatchers.resetMain()
    }
}
