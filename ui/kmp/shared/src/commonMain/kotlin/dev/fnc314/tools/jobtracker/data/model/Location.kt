package dev.fnc314.tools.jobtracker.data.model

data class Location(
    val city: String?,
    val region: String?,
    val country: String?,
    val workMode: WorkMode,
    val relocation: Boolean,
    val superCommuter: Boolean,
)
