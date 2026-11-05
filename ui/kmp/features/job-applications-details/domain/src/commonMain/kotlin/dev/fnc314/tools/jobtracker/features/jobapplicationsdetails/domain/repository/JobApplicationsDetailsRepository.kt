package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.repository

import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerEvent
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus

/**
 * Domain repository contract managing detail queries, status transitions, and audit events for a single application.
 */
public interface JobApplicationsDetailsRepository {

    /**
     * Retrieves an individual job application by its slug or UUID identifier.
     *
     * @param id The application slug or UUID.
     * @return The matched [JobApplication] or null if not found.
     */
    public suspend fun getJobApplication(id: String): JobApplication?

    /**
     * Retrieves the collection of known employer companies.
     *
     * @return List of [Company] profiles.
     */
    public suspend fun getCompanies(): List<Company>

    /**
     * Mutates the lifecycle status of an application, appending an audit event and updating timestamps.
     *
     * @param id The target application identifier.
     * @param status The new [TrackerStatus] to apply.
     * @param date Optional effective ISO date string.
     * @param note Optional context notes explaining the status change.
     * @return The updated [JobApplication] instance.
     */
    public suspend fun updateStatus(id: String, status: TrackerStatus, date: String?, note: String?): JobApplication

    /**
     * Appends an immutable audit event to the application's timeline subcollection.
     *
     * @param event The [TrackerEvent] to record.
     * @return The generated event identifier.
     */
    public suspend fun recordEvent(event: TrackerEvent): String

    /**
     * Updates the rubric fit score rating for an application.
     *
     * @param id The target application identifier.
     * @param score New fit score rating (0..100).
     * @return The updated [JobApplication] instance.
     */
    public suspend fun updateFitScore(id: String, score: Int): JobApplication

    /**
     * Requests a time-limited signed download URL for an artifact stored in Google Cloud Storage.
     *
     * @param path The cloud storage object path.
     * @return A signed HTTPS URL for direct artifact viewing.
     */
    public suspend fun getArtifactSignedUrl(path: String): String
}
