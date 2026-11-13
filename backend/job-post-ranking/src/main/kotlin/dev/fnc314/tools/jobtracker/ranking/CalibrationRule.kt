package dev.fnc314.tools.jobtracker.ranking

/**
 * Company-specific calibration rule for fit score adjustments.
 *
 * Source of truth: `.agents/skills/job-application-assistant/04-job-evaluation.md`
 *
 * @property maxScore Maximum score cap (e.g., Reddit capped at 75).
 * @property overrideScore Fixed score override (e.g., Capital One Mobile Engineer 4 → 95).
 *   Takes precedence over [maxScore] if both are set.
 */
public data class CalibrationRule(
    val maxScore: Int? = null,
    val overrideScore: Int? = null,
)

/**
 * Pre-configured calibration rules mapped by company slug.
 *
 * Update this map when new calibration data arrives from `04-job-evaluation.md`.
 */
public val CALIBRATION_RULES: Map<String, CalibrationRule> = mapOf(
    // Reddit: historically low conversion rate
    "reddit" to CalibrationRule(maxScore = 75),
    // Known low-conversion companies
    "playlist" to CalibrationRule(maxScore = 75),
    "instacart" to CalibrationRule(maxScore = 75),
    "sentilink" to CalibrationRule(maxScore = 75),
    "ulta-beauty" to CalibrationRule(maxScore = 75),
    "thomson-reuters" to CalibrationRule(maxScore = 75),
    "eight-sleep" to CalibrationRule(maxScore = 75),
    // Capital One Mobile Engineer 4: explicit high-match override
    "capital-one" to CalibrationRule(overrideScore = 95),
)
