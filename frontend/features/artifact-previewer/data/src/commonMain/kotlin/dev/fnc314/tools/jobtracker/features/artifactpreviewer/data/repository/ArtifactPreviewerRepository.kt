package dev.fnc314.tools.jobtracker.features.artifactpreviewer.data.repository

/**
 * Repository contract for fetching artifact content from remote URLs.
 */
public interface ArtifactPreviewerRepository {
    /**
     * Fetches text content from the given URL.
     *
     * @param url The URL to fetch.
     * @return The text content or null if the fetch failed.
     */
    public suspend fun fetchContent(url: String): String?
}
