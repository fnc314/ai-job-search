package dev.fnc314.aijobtracker.backend.scraping

import dev.fnc314.aijobtracker.backend.scraping.model.ScrapedJobPosting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow

/**
 * Service orchestrating multi-portal job scraping and vacancy deduplication.
 */
public class JobScraperService(
    private val scrapers: List<PortalScraper> = listOf(FreeHirePortalScraper())
) {
    /**
     * Executes aggregated scraping across registered scrapers.
     *
     * @param query Search query keyword.
     * @param location Target location.
     * @return Flow of unique [ScrapedJobPosting] items.
     */
    public fun scrapeAll(query: String, location: String): Flow<ScrapedJobPosting> = kotlinx.coroutines.flow.flow {
        val seenUrls = mutableSetOf<String>()
        for (scraper in scrapers) {
            scraper(query, location).collect { posting ->
                if (posting.url.isNotBlank() && seenUrls.add(posting.url)) {
                    emit(posting)
                }
            }
        }
    }
}
