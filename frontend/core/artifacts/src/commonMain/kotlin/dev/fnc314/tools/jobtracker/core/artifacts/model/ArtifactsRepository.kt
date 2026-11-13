package dev.fnc314.tools.jobtracker.core.artifacts.model

/**
 * Repository contract for fetching and caching job application artifacts.
 *
 * Provides local-cache-first access to [TrackerArtifact] records stored in the
 * `applications/{id}/artifacts` Firestore subcollection, with Room caching.
 */
public interface ArtifactsRepository {

    /**
     * Retrieves all attached artifacts for the specified job application.
     *
     * Queries the local Room cache first; falls back to Firestore if not cached.
     *
     * @param applicationId Unique application identifier or slug.
     * @return List of [TrackerArtifact] records associated with this application.
     */
    public suspend fun getArtifacts(applicationId: String): List<TrackerArtifact>

    /**
     * Retrieves a single artifact by its ID within the specified application.
     *
     * @param applicationId Unique application identifier or slug.
     * @param artifactId Unique artifact identifier.
     * @return The matched [TrackerArtifact] or null if not found.
     */
    public suspend fun getArtifact(applicationId: String, artifactId: String): TrackerArtifact?
}
