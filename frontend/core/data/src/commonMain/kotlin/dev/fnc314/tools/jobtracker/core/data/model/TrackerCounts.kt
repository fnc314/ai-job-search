package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Aggregated metric counters summarizing interaction frequency and progress for an application.
 *
 * @property events Total number of recorded events linked to this opportunity.
 * @property followUpsSent Number of follow-up messages sent by the candidate or agent.
 * @property interviews Number of interview rounds or screens scheduled or completed.
 * @property rejections Number of rejections recorded across all related application cycles.
 */
public data class TrackerCounts(
    public val events: Int,
    public val followUpsSent: Int,
    public val interviews: Int,
    public val rejections: Int,
)
