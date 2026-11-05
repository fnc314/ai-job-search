package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Structured compensation details and benchmark alignment for a job opportunity.
 *
 * @property min Lower boundary of the base salary or total target compensation range.
 * @property max Upper boundary of the base salary or total target compensation range.
 * @property currency Three-letter ISO 4217 currency code (default: `"USD"`).
 * @property period Compensation frequency interval (e.g. `"year"` or `"hour"`).
 * @property bandSource Attribution source for the salary data (e.g., `"posting"`, `"levels.fyi"`).
 * @property vsTarget Qualitative evaluation relative to candidate floor (e.g. `"above-target"` or `"below-floor"`).
 */
public data class Compensation(
    public val min: Double?,
    public val max: Double?,
    public val currency: String?,
    public val period: String?,
    public val bandSource: String?,
    public val vsTarget: String?,
)
