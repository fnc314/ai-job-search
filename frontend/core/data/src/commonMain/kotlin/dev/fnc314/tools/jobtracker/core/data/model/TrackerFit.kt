package dev.fnc314.tools.jobtracker.core.data.model

import kotlinx.collections.immutable.ImmutableList
import kotlin.time.Instant

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
 */
public data class TrackerFit(
    public val rubricVersion: Int = 1,
    public val score: Int? = null,
    public val evaluatedAt: Instant?,
    public val gaps: ImmutableList<String>,
)
