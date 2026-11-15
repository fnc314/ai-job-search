package dev.fnc314.tools.jobtracker.ranking

/**
 * Extracted job metadata from a posting.
 *
 * @property company Company name extracted from the posting.
 * @property role Role title extracted from the posting.
 * @property location Location string extracted from the posting.
 */
public data class JobInfo(
    /** Company name extracted from the posting. */
    public val company: String = "",
    /** Role title extracted from the posting. */
    public val role: String = "",
    /** Location string extracted from the posting. */
    public val location: String = "",
)
