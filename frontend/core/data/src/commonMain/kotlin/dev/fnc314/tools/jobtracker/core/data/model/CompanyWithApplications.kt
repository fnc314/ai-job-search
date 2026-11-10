package dev.fnc314.tools.jobtracker.core.data.model

/**
 * Domain entity combining a company profile and its associated job applications.
 */
public data class CompanyWithApplications(
    public val company: Company,
    public val applications: List<JobApplication>,
)
