package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data.source

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.source.RemoteDataSource

public interface RemoteJobApplicationsDetailsDataSource {
    public suspend fun getJobApplication(id: String): JobApplication?
    public suspend fun getCompanies(): List<Company>
    public suspend fun updateStatus(id: String, status: TrackerStatus, date: String?, note: String?): JobApplication
    public suspend fun recordEvent(event: TrackerEvent): String
    public suspend fun updateFitScore(id: String, score: Int): JobApplication
}

internal class RemoteJobApplicationsDetailsDataSourceImpl(
    private val remoteDataSource: RemoteDataSource? = null,
) : RemoteJobApplicationsDetailsDataSource {
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
