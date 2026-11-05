package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui

import dev.fnc314.tools.jobtracker.core.data.model.ApplicationChannel
import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEventType
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.GetArtifactSignedUrlUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.LoadCompaniesUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.LoadJobApplicationUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.RecordEventUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.UpdateApplicationStatusUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.UpdateFitScoreUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
internal class JobApplicationDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleApplication = JobApplication(
        id = "app_1",
        company = "Acme Corp",
        role = "Staff Android Engineer",
        location = "Remote",
        status = TrackerStatus.APPLIED,
        channel = ApplicationChannel.LINKEDIN,
        jobId = "job_1",
        jobUrl = "https://example.com/job/1",
        description = "Role description",
        tags = persistentListOf("Android", "Kotlin"),
        contactIds = persistentListOf(),
        compensation = null,
        locationDetail = null,
        fitRating = 85,
        fitEvaluation = null,
        counters = null,
        timeline = null,
        notesRendered = "First note\nSecond note",
        createdAt = Instant.fromEpochMilliseconds(1000L),
        updatedAt = Instant.fromEpochMilliseconds(2000L),
        jobApplicationUrl = "https://example.com/app/1",
    )

    private val sampleCompany = Company(
        id = "comp_1",
        name = "Acme Corp",
        aliases = listOf("Acme"),
        website = "https://acme.com",
        industry = "Technology",
        createdAt = null,
        updatedAt = null,
    )

    private class FakeLoadJobApplicationUseCase(
        var application: JobApplication? = null,
        var shouldThrow: Boolean = false,
    ) : LoadJobApplicationUseCase {
        override suspend fun invoke(params: LoadJobApplicationUseCase.Params): JobApplication? {
            if (shouldThrow) throw IllegalStateException("Network failure")
            return if (params.id == application?.id) application else null
        }
    }

    private class FakeLoadCompaniesUseCase(
        var companies: List<Company> = emptyList(),
    ) : LoadCompaniesUseCase {
        override suspend fun invoke(params: LoadCompaniesUseCase.Params): List<Company> = companies
    }

    private class FakeUpdateApplicationStatusUseCase(
        var updatedApplication: JobApplication? = null,
    ) : UpdateApplicationStatusUseCase {
        var lastParams: UpdateApplicationStatusUseCase.Params? = null
        override suspend fun invoke(params: UpdateApplicationStatusUseCase.Params): JobApplication {
            lastParams = params
            return updatedApplication ?: throw IllegalStateException("Not configured")
        }
    }

    private class FakeRecordEventUseCase : RecordEventUseCase {
        var lastRecordedEventId: String? = null
        override suspend fun invoke(params: RecordEventUseCase.Params): String {
            lastRecordedEventId = params.event.id
            return params.event.id
        }
    }

    private class FakeUpdateFitScoreUseCase(
        var updatedApplication: JobApplication? = null,
    ) : UpdateFitScoreUseCase {
        var lastParams: UpdateFitScoreUseCase.Params? = null
        override suspend fun invoke(params: UpdateFitScoreUseCase.Params): JobApplication {
            lastParams = params
            return updatedApplication ?: throw IllegalStateException("Not configured")
        }
    }

    private class FakeGetArtifactSignedUrlUseCase : GetArtifactSignedUrlUseCase {
        override suspend fun invoke(params: GetArtifactSignedUrlUseCase.Params): String {
            return "https://signed.url/${params.path}"
        }
    }

    private lateinit var loadAppUseCase: FakeLoadJobApplicationUseCase
    private lateinit var loadCompaniesUseCase: FakeLoadCompaniesUseCase
    private lateinit var updateStatusUseCase: FakeUpdateApplicationStatusUseCase
    private lateinit var recordEventUseCase: FakeRecordEventUseCase
    private lateinit var updateFitScoreUseCase: FakeUpdateFitScoreUseCase
    private lateinit var getSignedUrlUseCase: FakeGetArtifactSignedUrlUseCase
    private lateinit var viewModel: JobApplicationDetailViewModel

    @BeforeTest
    fun setUp(): Unit {
        Dispatchers.setMain(testDispatcher)
        loadAppUseCase = FakeLoadJobApplicationUseCase(application = sampleApplication)
        loadCompaniesUseCase = FakeLoadCompaniesUseCase(companies = listOf(sampleCompany))
        updateStatusUseCase = FakeUpdateApplicationStatusUseCase(
            updatedApplication = sampleApplication.copy(status = TrackerStatus.INTERVIEW)
        )
        recordEventUseCase = FakeRecordEventUseCase()
        updateFitScoreUseCase = FakeUpdateFitScoreUseCase(
            updatedApplication = sampleApplication.copy(fitRating = 95)
        )
        getSignedUrlUseCase = FakeGetArtifactSignedUrlUseCase()

        viewModel = JobApplicationDetailViewModel(
            loadApplicationUseCase = loadAppUseCase,
            loadCompaniesUseCase = loadCompaniesUseCase,
            updateStatusUseCase = updateStatusUseCase,
            recordEventUseCase = recordEventUseCase,
            updateFitScoreUseCase = updateFitScoreUseCase,
            getSignedUrlUseCase = getSignedUrlUseCase,
        )
    }

    @AfterTest
    fun tearDown(): Unit {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsLoading(): Unit = runTest(testDispatcher) {
        val state = viewModel.uiState.value
        assertEquals(JobApplicationDetailUiState.Loading, state)
    }

    @Test
    fun loadIntentEmitsContentOnSuccess(): Unit = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.onIntent(JobApplicationDetailUiIntent.Load("app_1"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<JobApplicationDetailUiState.Content>(state)
        assertEquals("Acme Corp", state.model.companyName)
        assertEquals("Staff Android Engineer", state.model.role)
        assertEquals(TrackerStatus.APPLIED, state.model.status)
        assertEquals(85, state.model.fitScore)
        assertEquals(2, state.model.notesList.size)
        assertTrue(state.model.timelineEvents.isNotEmpty())
        assertEquals(false, state.isUpdating)
    }

    @Test
    fun loadIntentEmitsErrorWhenNotFound(): Unit = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.onIntent(JobApplicationDetailUiIntent.Load("non_existent"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<JobApplicationDetailUiState.Error>(state)
        assertEquals("Application not found", state.message)
    }

    @Test
    fun updateStatusIntentUpdatesModel(): Unit = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.onIntent(JobApplicationDetailUiIntent.Load("app_1"))
        advanceUntilIdle()

        viewModel.onIntent(
            JobApplicationDetailUiIntent.UpdateStatus(
                newStatus = TrackerStatus.INTERVIEW,
                note = "Passed screening",
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<JobApplicationDetailUiState.Content>(state)
        assertEquals(TrackerStatus.INTERVIEW, state.model.status)
        assertEquals("Interview", state.model.statusDisplayText)
        assertEquals(TrackerStatus.INTERVIEW, updateStatusUseCase.lastParams?.status)
        assertEquals("Passed screening", updateStatusUseCase.lastParams?.note)
    }

    @Test
    fun updateFitScoreIntentUpdatesModel(): Unit = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.onIntent(JobApplicationDetailUiIntent.Load("app_1"))
        advanceUntilIdle()

        viewModel.onIntent(JobApplicationDetailUiIntent.UpdateFitScore(newScore = 95))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<JobApplicationDetailUiState.Content>(state)
        assertEquals(95, state.model.fitScore)
        assertEquals("95", state.model.fitScoreDisplayText)
    }

    @Test
    fun recordEventIntentAppendsTimelineEvent(): Unit = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.onIntent(JobApplicationDetailUiIntent.Load("app_1"))
        advanceUntilIdle()

        val initialEventCount = (viewModel.uiState.value as JobApplicationDetailUiState.Content).model.timelineEvents.size

        viewModel.onIntent(
            JobApplicationDetailUiIntent.RecordEvent(
                type = TrackerEventType.RECRUITER_SCREEN,
                notes = "Screen went very well",
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertIs<JobApplicationDetailUiState.Content>(state)
        assertEquals(initialEventCount + 1, state.model.timelineEvents.size)
        assertTrue(recordEventUseCase.lastRecordedEventId != null)
    }

    @Test
    fun clickBackIntentEmitsNavigateBack(): Unit = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.uiState.collect() }

        viewModel.onIntent(JobApplicationDetailUiIntent.ClickBack)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(JobApplicationDetailUiState.NavigateBack, state)
    }
}
