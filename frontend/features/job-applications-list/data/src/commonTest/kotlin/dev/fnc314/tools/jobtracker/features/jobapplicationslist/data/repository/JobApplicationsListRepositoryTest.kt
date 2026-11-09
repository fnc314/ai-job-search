package dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.repository

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.repository.impl.JobApplicationsListRepositoryImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source.LocalJobApplicationsListDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source.RemoteJobApplicationsListDataSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class JobApplicationsListRepositoryTest {

    internal class FakeLocalJobApplicationsListDataSource : LocalJobApplicationsListDataSource {
        var applications = mutableListOf<JobApplication>()
        var clearAndSaveCalled = false

        override suspend fun getJobApplications(): List<JobApplication> = applications.toList()
        override suspend fun saveJobApplications(applications: List<JobApplication>) {
            this.applications.clear()
            this.applications.addAll(applications)
        }

        override suspend fun clearAndSaveJobApplications(applications: List<JobApplication>) {
            clearAndSaveCalled = true
            this.applications.clear()
            this.applications.addAll(applications)
        }
    }

    internal class FakeRemoteJobApplicationsListDataSource : RemoteJobApplicationsListDataSource {
        var remoteApps = listOf(
            Factory.createJobApplication(id = "app-1", company = "Google"),
            Factory.createJobApplication(id = "app-2", company = "Apple"),
        )

        override suspend operator fun invoke(): List<JobApplication> = remoteApps
    }

    @Test
    fun testRepositoryImpl_ReturnsLocalCacheWhenAvailable() = runTest {
        val local = FakeLocalJobApplicationsListDataSource().apply {
            applications.add(Factory.createJobApplication(id = "local-1", company = "LocalCo"))
        }
        val remote = FakeRemoteJobApplicationsListDataSource()
        val repo = JobApplicationsListRepositoryImpl(remote, local)

        val result = repo()
        assertEquals(1, result.size)
        assertEquals("local-1", result.first().id)
    }

    @Test
    fun testRepositoryImpl_FetchesRemoteAndCachesWhenLocalEmpty() = runTest {
        val local = FakeLocalJobApplicationsListDataSource()
        val remote = FakeRemoteJobApplicationsListDataSource()
        val repo = JobApplicationsListRepositoryImpl(remote, local)

        val result = repo()
        assertEquals(2, result.size)
        assertEquals(2, local.applications.size)
        assertEquals("app-1", local.applications.first().id)
    }

    @Test
    fun testRepositoryImpl_HandlesEmptyRemote() = runTest {
        val local = FakeLocalJobApplicationsListDataSource()
        val remote = FakeRemoteJobApplicationsListDataSource().apply { remoteApps = emptyList() }
        val repo = JobApplicationsListRepositoryImpl(remote, local)

        val result = repo()
        assertEquals(0, result.size)
        assertEquals(0, local.applications.size)
    }

    @Test
    fun testRepositoryImpl_CacheIntegrityAcrossCalls() = runTest {
        val local = FakeLocalJobApplicationsListDataSource()
        val remote = FakeRemoteJobApplicationsListDataSource()
        val repo = JobApplicationsListRepositoryImpl(remote, local)

        val firstCall = repo()
        assertEquals(2, firstCall.size)
        remote.remoteApps = emptyList()
        val secondCall = repo()
        assertEquals(2, secondCall.size)
    }

    @Test
    fun testRepositoryImpl_ForceRefreshFetchesRemoteAndClearsLocal() = runTest {
        val local = FakeLocalJobApplicationsListDataSource().apply {
            applications.add(Factory.createJobApplication(id = "old-local", company = "OldCo"))
        }
        val remote = FakeRemoteJobApplicationsListDataSource()
        val repo = JobApplicationsListRepositoryImpl(remote, local)

        val result = repo(forceRefresh = true)
        assertEquals(2, result.size)
        assertEquals("app-1", result.first().id)
        assertEquals(2, local.applications.size)
        assertEquals("app-1", local.applications.first().id)
        assertEquals(true, local.clearAndSaveCalled)
    }
}
