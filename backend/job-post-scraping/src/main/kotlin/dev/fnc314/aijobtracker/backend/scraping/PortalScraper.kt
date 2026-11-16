package dev.fnc314.aijobtracker.backend.scraping

import dev.fnc314.aijobtracker.backend.scraping.model.ScrapedJobPosting
import kotlinx.coroutines.flow.Flow

/**
 * Single Abstract Method (SAM) functional interface for portal-specific job scraping execution.
 */
public fun interface PortalScraper {
    /**
     * Executes job vacancy scraping for the target search parameters.
     *
     * @param query Search query term (e.g. role title or technology stack).
     * @param location Geographic location or remote constraint.
     * @return Flow emitting discovered [ScrapedJobPosting] instances.
     */
    public operator fun invoke(
        query: String,
        location: String
    ): Flow<ScrapedJobPosting>
}
