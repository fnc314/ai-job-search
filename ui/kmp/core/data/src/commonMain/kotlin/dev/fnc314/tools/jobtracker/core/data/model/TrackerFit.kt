package dev.fnc314.tools.jobtracker.core.data.model

import kotlinx.collections.immutable.ImmutableList
import kotlin.time.Instant

public data class TrackerFit(
    public val rubricVersion: Int = 1,
    public val score: Int? = null,
    public val evaluatedAt: Instant?,
    public val gaps: ImmutableList<String>,
)
