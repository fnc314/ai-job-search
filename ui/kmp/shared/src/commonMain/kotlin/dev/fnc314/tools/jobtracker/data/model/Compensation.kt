package dev.fnc314.tools.jobtracker.data.model

data class Compensation(
    val min: Double?,
    val max: Double?,
    val currency: String?,
    val period: String?,
    val bandSource: String?,
    val vsTarget: String?,
)
