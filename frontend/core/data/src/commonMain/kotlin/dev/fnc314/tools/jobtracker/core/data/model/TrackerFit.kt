package dev.fnc314.tools.jobtracker.core.data.model

import kotlinx.collections.immutable.ImmutableList
import kotlin.time.Instant

/**
 * Dynamic weights configuration for deterministic fit scoring.
 *
 * @property technical Weight for technical keyword matching (default 0.30).
 * @property experience Weight for experience keyword matching (default 0.25).
 * @property behavioral Weight for behavioral/cultural keyword matching (default 0.15).
 * @property career Weight for career alignment keyword matching (default 0.30).
 * @property roleLevelMultiplier Multiplier for role-level detection bonuses (default 1.0).
 * @property sectorMultiplier Multiplier for sector alignment bonuses (default 1.0).
 * @property locationMultiplier Multiplier for location alignment bonuses (default 1.0).
 */
public data class ScoringWeights(
    public val technical: Double = 0.30,
    public val experience: Double = 0.25,
    public val behavioral: Double = 0.15,
    public val career: Double = 0.30,
    public val roleLevelMultiplier: Double = 1.0,
    public val sectorMultiplier: Double = 1.0,
    public val locationMultiplier: Double = 1.0,
) {
    public companion object {
        public val DEFAULT: ScoringWeights = ScoringWeights()
    }
}

/**
 * Objective candidate-to-role fit evaluation and gap assessment against verified candidate experience.
 *
 * Grounded in the candidate profile (`01-candidate-profile.md`), this model encapsulates rubric scores
 * and technical or geographical gap tags.
 *
 * @property rubricVersion Evaluation rubric algorithm version.
 * @property score Calculated alignment score on a scale from 0 to 100.
 * @property evaluatedAt Timestamp when the fit assessment was computed.
 * @property gaps List of identified alignment deficiencies (e.g. `"fit:gap-ios"`, `"compensation:below-band"`).
 * @property weights Dynamic scoring weights used for this evaluation.
 */
public data class TrackerFit(
    public val rubricVersion: Int = 1,
    public val score: Int? = null,
    public val evaluatedAt: Instant?,
    public val gaps: ImmutableList<String>,
    public val weights: ScoringWeights? = null,
)
