package dev.fnc314.aijobtracker.backend.scraping.model

import kotlinx.serialization.Serializable

/**
 * Clean data model representing a scraped job vacancy from any supported portal.
 *
 * @property id Unique persistent identifier or URL hash for the posting.
 * @property portal Source portal from which the vacancy was retrieved (e.g. LinkedIn, FreeHire, Jobnet).
 * @property title Target position or role title.
 * @property company Employer or company name.
 * @property location Geographic location or remote status.
 * @property url Public canonical URL of the posting.
 * @property description Full extracted text content of the job description.
 * @property datePosted Optional date string when the vacancy was published.
 */
@Serializable
public data class ScrapedJobPosting(
    val id: String,
    val portal: String,
    val title: String,
    val company: String,
    val location: String,
    val url: String,
    val description: String,
    val datePosted: String? = null
)
