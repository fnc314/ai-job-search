package dev.fnc314.tools.jobtracker.ranking

import kotlinx.serialization.Serializable

/**
 * Result of a deterministic job ranking evaluation.
 *
 * Combines keyword-based dimension scoring with role-level detection,
 * sector alignment, location scoring, calibration rules, and deal-breaker penalties.
 */
@Serializable
public data class RankResult(
    val overall: Int,
    val verdict: String,
    val technical: Int,
    val experience: Int,
    val behavioral: Int,
    val career: Int,
    val deal_breakers: List<String>,
    val strengths: List<String>,
    val gaps: List<String>,
    val company: String,
    val role: String,
    val locationVerdict: String = "",
)
