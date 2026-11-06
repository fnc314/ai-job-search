package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Geographic work location specifications and commute requirements for an opportunity.
 *
 * @property city Municipal location or metropolitan area (e.g. `"Pittsburgh"`).
 * @property region State, province, or market zone (e.g., `"PA"`, `"NYC"`).
 * @property country Country code or nation name (default: `"US"`).
 * @property roleLocation Remote work policy and location arrangement [RoleLocation] (e.g., Remote, Hybrid, or Onsite).
 * @property relocation Whether employer requires or provides relocation to the office site.
 * @property superCommuter Flag indicating whether role permits super-commuting (periodic travel to NYC/DC).
 * @see RoleLocation
 */
public data class Location(
    public val city: String?,
    public val region: String?,
    public val country: String?,
    public val roleLocation: RoleLocation,
    public val relocation: Boolean,
    public val superCommuter: Boolean,
) {
    /** Backward-compatible accessor for [roleLocation]. */
    public val workMode: RoleLocation get() = roleLocation
}
