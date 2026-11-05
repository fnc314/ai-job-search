package dev.fnc314.tools.jobtracker.core.data.model

public data class Compensation(
    public val min: Double?,
    public val max: Double?,
    public val currency: String?,
    public val period: String?,
    public val bandSource: String?,
    public val vsTarget: String?,
)
