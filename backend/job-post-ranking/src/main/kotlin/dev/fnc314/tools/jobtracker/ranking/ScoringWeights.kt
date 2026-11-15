package dev.fnc314.tools.jobtracker.ranking

import kotlinx.serialization.Serializable

/**
 * Dynamic weights configuration for the deterministic job fit scorer.
 *
 * Users can adjust dimension weights to influence scoring without manually overriding
 * the fit value. Weights must sum to 1.0.
 *
 * @property technical Weight for technical keyword matching (default 0.30).
 * @property experience Weight for experience keyword matching (default 0.25).
 * @property behavioral Weight for behavioral/cultural keyword matching (default 0.15).
 * @property career Weight for career alignment keyword matching (default 0.30).
 * @property roleLevelMultiplier Multiplier for role-level detection bonuses (default 1.0).
 * @property sectorMultiplier Multiplier for sector alignment bonuses (default 1.0).
 * @property locationMultiplier Multiplier for location alignment bonuses (default 1.0).
 */
@Serializable
public data class ScoringWeights(
    /** Weight for technical keyword matching (default 0.30). */
    public val technical: Double = 0.30,
    /** Weight for experience keyword matching (default 0.25). */
    public val experience: Double = 0.25,
    /** Weight for behavioral/cultural keyword matching (default 0.15). */
    public val behavioral: Double = 0.15,
    /** Weight for career alignment keyword matching (default 0.30). */
    public val career: Double = 0.30,
    /** Multiplier for role-level detection bonuses (default 1.0). */
    public val roleLevelMultiplier: Double = 1.0,
    /** Multiplier for sector alignment bonuses (default 1.0). */
    public val sectorMultiplier: Double = 1.0,
    /** Multiplier for location alignment bonuses (default 1.0). */
    public val locationMultiplier: Double = 1.0,
) {
    /**
     * Predefined presets for scoring weight configurations.
     */
    public companion object {
        /**
         * Default baseline scoring weights reflecting standard candidate profile calibration.
         */
        public val DEFAULT: ScoringWeights = ScoringWeights()
    }

    /**
     * Validate that dimension weights sum to 1.0 and all values are non-negative.
     * @throws IllegalArgumentException if weights are invalid.
     */
    public fun validate(): Unit {
        val sum = technical + experience + behavioral + career
        if (sum < 0.99 || sum > 1.01) {
            throw IllegalArgumentException(
                "Dimension weights must sum to 1.0, got $sum " +
                    "(technical=$technical, experience=$experience, behavioral=$behavioral, career=$career)"
            )
        }
        if (technical < 0 || experience < 0 || behavioral < 0 || career < 0) {
            throw IllegalArgumentException("Dimension weights must be non-negative")
        }
        if (roleLevelMultiplier < 0 || sectorMultiplier < 0 || locationMultiplier < 0) {
            throw IllegalArgumentException("Multipliers must be non-negative")
        }
    }
}
