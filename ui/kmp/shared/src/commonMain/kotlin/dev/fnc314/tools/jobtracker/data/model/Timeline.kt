package dev.fnc314.tools.jobtracker.data.model

import kotlin.time.Instant

data class Timeline(
    val firstActivityAt: Instant?,
    val lastActivityAt: Instant?,
    val daysSinceActivity: Int,
    val nextFollowUpAt: Instant?,
)
