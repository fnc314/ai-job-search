package dev.fnc314.tools.jobtracker.data.model

import kotlin.time.Instant

data class TrackerFit(
    val rubricVersion: Int,
    val score: Int? = null,
    val evaluatedAt: Instant?,
    val gaps: List<String>,
)
