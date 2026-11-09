package dev.fnc314.tools.jobtracker.features.jobapplicationslist.ui

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.FilterJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.FilterJobApplicationsUseCase.ApplicationAge
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.LoadAllJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.SortJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.SortJobApplicationsUseCase.SortColumn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
internal class JobApplicationsListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleApps: List<JobApplication> = listOf(
        Factory.createJobApplication(
            id = "app-1",
            company = "Acme Corp",
            role = "Staff Android Engineer",
            status = TrackerStatus.APPLIED,
        ),
        Factory.createJobApplication(
            id = "app-2",
            company = "Beta Inc",
            role = "Principal Architect",
            status = TrackerStatus.INTERVIEW,
        ),
    )

    private val fakeLoadAll = object : LoadAllJobApplicationsUseCase {
        var shouldFail: Boolean = false
        override suspend fun invoke(params: LoadAllJobApplicationsUseCase.Params): List<JobApplication> {
            if (shouldFail) throw IllegalStateException("Load failed")
            return sampleApps
        }
    }

    private val fakeFilter = object : FilterJobApplicationsUseCase {
        override suspend fun invoke(params: FilterJobApplicationsUseCase.Params): List<JobApplication> {
            return params.applications.filter { app ->
                if (params.status != TrackerStatus.NONE && app.status != params.status) false else true
            }
        }
    }

    private val fakeSort = object : SortJobApplicationsUseCase {
        override suspend fun invoke(params: SortJobApplicationsUseCase.Params): List<JobApplication> {
            return if (params.ascending) {
                params.applications.sortedBy { it.company }
            } else {
                params.applications.sortedByDescending { it.company }
            }
        }
    }

    @BeforeTest
    fun setUp(): Unit {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown(): Unit {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoad_Success(): Unit = runTest(testDispatcher) {
        val viewModel = JobApplicationsListViewModel(
            loadAll = fakeLoadAll,
            filterApplications = fakeFilter,
            sortApplications = fakeSort,
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<JobApplicationsListUiState.Content>(state)
        assertEquals(2, state.applications.size)
        assertEquals("Beta Inc", state.applications[0].companyName)
        assertEquals("Acme Corp", state.applications[1].companyName)
        assertEquals(TrackerStatus.NONE, state.statusFilter)
        assertEquals(ApplicationAge.NONE, state.ageFilter)
    }

    @Test
    fun testInitialLoad_Failure(): Unit = runTest(testDispatcher) {
        fakeLoadAll.shouldFail = true
        val viewModel = JobApplicationsListViewModel(
            loadAll = fakeLoadAll,
            filterApplications = fakeFilter,
            sortApplications = fakeSort,
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<JobApplicationsListUiState.Error>(state)
        assertEquals("Load failed", state.message)
        fakeLoadAll.shouldFail = false
    }

    @Test
    fun testSelectApplication_EmitsNavigateToDetail(): Unit = runTest(testDispatcher) {
        val viewModel = JobApplicationsListViewModel(
            loadAll = fakeLoadAll,
            filterApplications = fakeFilter,
            sortApplications = fakeSort,
        )
        advanceUntilIdle()

        viewModel.onIntent(JobApplicationsListUiIntent.SelectApplication("app-123"))

        val state = viewModel.uiState.value
        assertIs<JobApplicationsListUiState.NavigateToDetail>(state)
        assertEquals("app-123", state.applicationId)
    }

    @Test
    fun testNavigationIntents(): Unit = runTest(testDispatcher) {
        val viewModel = JobApplicationsListViewModel(
            loadAll = fakeLoadAll,
            filterApplications = fakeFilter,
            sortApplications = fakeSort,
        )
        advanceUntilIdle()

        viewModel.onIntent(JobApplicationsListUiIntent.ClickAddApplication)
        assertEquals(JobApplicationsListUiState.NavigateToAddApplication, viewModel.uiState.value)

        viewModel.onIntent(JobApplicationsListUiIntent.ClickAddLead)
        assertEquals(JobApplicationsListUiState.NavigateToAddLead, viewModel.uiState.value)

        viewModel.onIntent(JobApplicationsListUiIntent.ClickEdits)
        assertEquals(JobApplicationsListUiState.NavigateToEdits, viewModel.uiState.value)

        viewModel.onIntent(JobApplicationsListUiIntent.ClickSettings)
        assertEquals(JobApplicationsListUiState.NavigateToSettings, viewModel.uiState.value)
    }

    @Test
    fun testChangeFilter_UpdatesContent(): Unit = runTest(testDispatcher) {
        val viewModel = JobApplicationsListViewModel(
            loadAll = fakeLoadAll,
            filterApplications = fakeFilter,
            sortApplications = fakeSort,
        )
        advanceUntilIdle()

        viewModel.onIntent(
            JobApplicationsListUiIntent.ChangeFilter(
                status = TrackerStatus.APPLIED,
                age = ApplicationAge.LESS_THAN_1_MONTH,
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<JobApplicationsListUiState.Content>(state)
        assertEquals(TrackerStatus.APPLIED, state.statusFilter)
        assertEquals(ApplicationAge.LESS_THAN_1_MONTH, state.ageFilter)
        assertEquals(1, state.applications.size)
        assertEquals("Acme Corp", state.applications[0].companyName)
    }

    @Test
    fun testChangeSort_UpdatesContent(): Unit = runTest(testDispatcher) {
        val viewModel = JobApplicationsListViewModel(
            loadAll = fakeLoadAll,
            filterApplications = fakeFilter,
            sortApplications = fakeSort,
        )
        advanceUntilIdle()

        viewModel.onIntent(
            JobApplicationsListUiIntent.ChangeSort(
                column = SortColumn.COMPANY,
                ascending = true,
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<JobApplicationsListUiState.Content>(state)
        assertTrue(state.sortAscending)
        assertEquals(SortColumn.COMPANY, state.sortColumn)
        assertEquals("Acme Corp", state.applications[0].companyName)
        assertEquals("Beta Inc", state.applications[1].companyName)
    }

    @Test
    fun testUiModel_MissingDataAndDraftingAppliedDateFallbacks(): Unit {
        val createdInstant = Instant.parse("2026-10-15T12:00:00Z")
        val draftingModel = JobApplicationItemUiModel(
            id = "draft-1",
            companyName = "hireeazy",
            roleTitle = "Staff Android",
            locationText = null,
            status = TrackerStatus.DRAFTING,
            statusDisplayName = "Drafting",
            fitScore = null,
            fitScoreText = null,
            createdAt = createdInstant,
            updatedAt = Instant.DISTANT_PAST,
            firstActivityAt = Instant.DISTANT_PAST,
            onClickIntent = JobApplicationsListUiIntent.SelectApplication("draft-1"),
        )

        assertFalse(draftingModel.hasBeenSubmitted)
        assertEquals("H", draftingModel.companyInitial)
        assertEquals("2026-10-15", draftingModel.createdDateOrNull())
        assertNull(draftingModel.appliedDateOrNull())
        assertEquals("Not applied", draftingModel.appliedDateDisplay("Not applied"))
        assertNull(draftingModel.fitScoreOrNull())
        assertEquals("Not evaluated", draftingModel.fitScoreDisplay("Not evaluated"))

        val appliedModel = draftingModel.copy(
            status = TrackerStatus.APPLIED,
            statusDisplayName = "Applied",
        )
        assertTrue(appliedModel.hasBeenSubmitted)
        assertEquals("2026-10-15", appliedModel.appliedDateOrNull())

        val blankCompanyModel = draftingModel.copy(
            companyName = "   ",
            createdAt = Instant.DISTANT_PAST,
        )
        assertEquals("?", blankCompanyModel.companyInitial)
        assertNull(blankCompanyModel.createdDateOrNull())
        assertEquals("—", blankCompanyModel.createdDateDisplay("—"))
    }
}
