package dev.fnc314.tools.jobtracker.data.firestore

import dev.fnc314.tools.jobtracker.data.model.*
import dev.fnc314.tools.jobtracker.di.Factory
import dev.fnc314.tools.jobtracker.domain.repository.JobApplicationRepository
import kotlin.time.Instant

internal class FirestoreJobApplicationRepository : JobApplicationRepository {

    override suspend fun getAllJobApplications(): List<JobApplication> {
        // TODO: Implement Firestore query
        return emptyList()
    }

    override suspend fun getJobApplication(id: String): JobApplication? {
        // TODO: Implement Firestore document fetch
        return null
    }

    override suspend fun getCompanies(): List<Company> {
        // TODO: Implement Firestore query
        return emptyList()
    }

    override suspend fun saveJobApplication(app: JobApplication): String {
        // TODO: Implement Firestore document write
        return app.id
    }

    override suspend fun updateStatus(
        id: String,
        status: TrackerStatus,
        date: String?,
        note: String?
    ): JobApplication {
        // TODO: Implement Firestore update
        return Factory.createJobApplication(
            id = id,
            company = "",
            status = status,
        )
    }

    override suspend fun recordEvent(event: TrackerEvent): String {
        // TODO: Implement Firestore subcollection write
        return event.id
    }

    override suspend fun updateFitScore(id: String, score: Int): JobApplication {
        // TODO: Implement Firestore update
        return Factory.createJobApplication(
            id = id,
            company = "",
            fitEvaluation = Factory.createTrackerFit(rubricVersion = score),
        )
    }
}
