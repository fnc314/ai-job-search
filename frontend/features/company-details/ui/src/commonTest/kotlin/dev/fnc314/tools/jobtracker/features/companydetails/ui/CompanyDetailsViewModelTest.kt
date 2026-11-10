package dev.fnc314.tools.jobtracker.features.companydetails.ui

import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.CompanyMetrics
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.CompanyWithApplications
import dev.fnc314.tools.jobtracker.features.companydetails.data.repository.CompanyDetailsRepository
import dev.fnc314.tools.jobtracker.features.companydetails.domain.LoadCompanyDetailsUseCase
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
class CompanyDetailsViewModelTest {

    private val sampleCompany = Company(
        id = "google",
        name = "Google",
        aliases = emptyList(),
        website = "https://google.com",
        industry = "Tech",
        createdAt = null,
        updatedAt = null,
        metrics = CompanyMetrics(
            counts = CompanyMetrics.ApplicationCounts(total = 2, submitted = 1),
            averageFitScore = 95.0,
        ),
    )

    private val sampleApplications = listOf(
        dev.fnc314.tools.jobtracker.core.data.factory.Factory.createJobApplication(
            id = "google-staff-android",
            company = "Google",
            role = "Staff Android Engineer",
        ),
        dev.fnc314.tools.jobtracker.core.data.factory.Factory.createJobApplication(
            id = "google-lead-architect",
            company = "Google",
            role = "Mobile Architect",
        ),
    )

    @Test
    fun testLoadCompany_Success() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)

        val mockRepo = object : CompanyDetailsRepository {
            override suspend fun getCompanyDetails(companyId: String): CompanyWithApplications? {
                return if (companyId == "google") {
                    CompanyWithApplications(sampleCompany, sampleApplications)
                } else null
            }
        }

        val viewModel = CompanyDetailsViewModel(LoadCompanyDetailsUseCase(mockRepo))
        viewModel.loadCompany("google")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CompanyDetailsUiState.Content)
        assertEquals("Google", state.details.companyName)
        assertEquals(2, state.details.applications.size)
        assertEquals("Staff Android Engineer", state.details.applications[0].roleTitle)

        Dispatchers.resetMain()
    }

    @Test
    fun testLoadCompany_NotFound() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)

        val mockRepo = object : CompanyDetailsRepository {
            override suspend fun getCompanyDetails(companyId: String): CompanyWithApplications? = null
        }

        val viewModel = CompanyDetailsViewModel(LoadCompanyDetailsUseCase(mockRepo))
        viewModel.loadCompany("nonexistent")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CompanyDetailsUiState.Error)
        assertEquals("Company not found", state.message)

        Dispatchers.resetMain()
    }
}
