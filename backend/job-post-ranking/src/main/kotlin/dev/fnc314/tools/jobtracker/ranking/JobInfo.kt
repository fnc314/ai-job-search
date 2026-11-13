package dev.fnc314.tools.jobtracker.ranking

/**
 * Extracted job metadata from a posting.
 *
 * @property company Company name extracted from the posting.
 * @property role Role title extracted from the posting.
 * @property location Location string extracted from the posting.
 */
public data class JobInfo(
    val company: String = "",
    val role: String = "",
    val location: String = "",
)
