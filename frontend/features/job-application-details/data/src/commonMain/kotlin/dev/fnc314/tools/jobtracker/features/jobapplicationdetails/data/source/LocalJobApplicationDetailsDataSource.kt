package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source

import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerArtifact

internal interface LocalJobApplicationDetailsDataSource {
    suspend fun getJobApplication(id: String): JobApplication?
    suspend fun saveJobApplication(application: JobApplication)
    suspend fun getCompanies(): List<Company>
    suspend fun saveCompanies(companies: List<Company>)
    suspend fun getArtifacts(applicationId: String): List<TrackerArtifact>?
    suspend fun saveArtifacts(applicationId: String, artifacts: List<TrackerArtifact>)
}
