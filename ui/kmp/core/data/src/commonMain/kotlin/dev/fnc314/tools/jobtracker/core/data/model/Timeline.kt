package dev.fnc314.tools.jobtracker.core.data.model

import kotlin.time.Instant

public data class Timeline(
    public val firstActivityAt: Instant?,
    public val lastActivityAt: Instant?,
    public val daysSinceActivity: Int,
    public val nextFollowUpAt: Instant?,
)
