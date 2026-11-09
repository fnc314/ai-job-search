package dev.fnc314.tools.jobtracker.core.data.model

import kotlin.time.Instant

/**
 * Key temporal activity milestones tracking candidate and company engagement pace.
 *
 * Missing timestamps are represented by [Instant.DISTANT_PAST] rather than null.
 *
 * @property firstActivityAt Timestamp of initial outreach, application submission, or discovery.
 * @property lastActivityAt Timestamp of the most recent bidirectional communication or event.
 * @property daysSinceActivity Elapsed days since [lastActivityAt] relative to the current evaluation date.
 * @property nextFollowUpAt Recommended or scheduled future date for sending follow-up communications.
 */
public data class Timeline(
    public val firstActivityAt: Instant = Instant.DISTANT_PAST,
    public val lastActivityAt: Instant = Instant.DISTANT_PAST,
    public val daysSinceActivity: Int = 0,
    public val nextFollowUpAt: Instant = Instant.DISTANT_PAST,
)
