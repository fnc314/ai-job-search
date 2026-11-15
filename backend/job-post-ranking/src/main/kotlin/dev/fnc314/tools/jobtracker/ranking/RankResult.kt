package dev.fnc314.tools.jobtracker.ranking

import kotlinx.serialization.Serializable

/**
 * Result of a deterministic job ranking evaluation.
 *
 * Combines keyword-based dimension scoring with role-level detection,
 * sector alignment, location scoring, calibration rules, and deal-breaker penalties.
 *
 * @property overall The overall calculated match score (0-100).
 * @property verdict The qualitative fit assessment verdict (e.g. STRONG_FIT, GOOD_FIT, etc.).
 * @property technical The technical dimension match score (0-100).
 * @property experience The experience dimension match score (0-100).
 * @property behavioral The behavioral and leadership dimension match score (0-100).
 * @property career The career goals and domain alignment match score (0-100).
 * @property deal_breakers List of identified disqualifiers or target mismatches.
 * @property strengths List of identified candidate strengths relevant to the posting.
 * @property gaps List of identified candidate skill or experience gaps.
 * @property company The normalized company name.
 * @property role The target job role title.
 * @property locationVerdict The geographical location alignment evaluation verdict.
 */
@Serializable
public data class RankResult(
    /** The overall calculated match score (0-100). */
    public val overall: Int,
    /** The qualitative fit assessment verdict (e.g. STRONG_FIT, GOOD_FIT, etc.). */
    public val verdict: String,
    /** The technical dimension match score (0-100). */
    public val technical: Int,
    /** The experience dimension match score (0-100). */
    public val experience: Int,
    /** The behavioral and leadership dimension match score (0-100). */
    public val behavioral: Int,
    /** The career goals and domain alignment match score (0-100). */
    public val career: Int,
    /** List of identified disqualifiers or target mismatches. */
    public val deal_breakers: List<String>,
    /** List of identified candidate strengths relevant to the posting. */
    public val strengths: List<String>,
    /** List of identified candidate skill or experience gaps. */
    public val gaps: List<String>,
    /** The normalized company name. */
    public val company: String,
    /** The target job role title. */
    public val role: String,
    /** The geographical location alignment evaluation verdict. */
    public val locationVerdict: String = "",
)
