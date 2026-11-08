package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.impl

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.source.RemoteDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.RemoteJobApplicationDetailsDataSource

@Suppress("UnusedPrivateProperty")
internal class RemoteJobApplicationDetailsDataSourceImpl(
    private val remoteDataSource: RemoteDataSource? = null,
) : RemoteJobApplicationDetailsDataSource {
    override suspend fun getJobApplication(id: String): JobApplication? = null
    override suspend fun getCompanies(): List<Company> = emptyList()
    override suspend fun updateStatus(id: String, status: TrackerStatus, date: String?, note: String?): JobApplication {
        return Factory.createJobApplication(
            id = id,
            company = "",
            status = status,
        )
    }
    override suspend fun recordEvent(event: TrackerEvent): String = event.id
    override suspend fun updateFitScore(id: String, score: Int): JobApplication {
        return Factory.createJobApplication(
            id = id,
            company = "",
            fitRating = score,
            fitEvaluation = Factory.createTrackerFit(score = score),
        )
    }
}
