package dev.fnc314.tools.jobtracker.core.data.model

public data class Location(
    public val city: String?,
    public val region: String?,
    public val country: String?,
    public val workMode: WorkMode,
    public val relocation: Boolean,
    public val superCommuter: Boolean,
)
