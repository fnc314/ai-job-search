package dev.fnc314.tools.jobtracker.core.data.repository

import dev.fnc314.tools.jobtracker.core.data.factory.Factory
import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus

public class RemoteJobApplicationRepository : JobApplicationRepository {

    override suspend fun getAllJobApplications(): List<JobApplication> {
        // Default remote query stub
        return emptyList()
    }

    override suspend fun getJobApplication(id: String): JobApplication? {
        // Default remote document fetch stub
        return null
    }

    override suspend fun getCompanies(): List<Company> {
        // Default remote query stub
        return emptyList()
    }

    override suspend fun saveJobApplication(app: JobApplication): String {
        // Default remote document write stub
        return app.id
    }

    override suspend fun updateStatus(
        id: String,
        status: TrackerStatus,
        date: String?,
        note: String?
    ): JobApplication {
        // Default remote update stub
        return Factory.createJobApplication(
            id = id,
            company = "",
            status = status,
        )
    }

    override suspend fun recordEvent(event: TrackerEvent): String {
        // Default remote event write stub
        return event.id
    }

    override suspend fun updateFitScore(id: String, score: Int): JobApplication {
        // Default remote update stub
        return Factory.createJobApplication(
            id = id,
            company = "",
            fitRating = score,
            fitEvaluation = Factory.createTrackerFit(score = score),
        )
    }
}
