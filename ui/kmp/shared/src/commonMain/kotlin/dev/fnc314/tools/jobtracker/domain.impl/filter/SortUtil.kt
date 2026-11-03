package dev.fnc314.tools.jobtracker.domain.impl.filter

import dev.fnc314.tools.jobtracker.data.model.JobApplication

object SortUtil {

    fun sortByDate(applications: List<JobApplication>, ascending: Boolean): List<JobApplication> =
        applications.sortedWith(compareBy<JobApplication> { it.createdAt ?: kotlin.time.Instant.DISTANT_PAST }.thenBy { it.company })
            .let { if (ascending) it else it.reversed() }

    fun sortByCompany(applications: List<JobApplication>, ascending: Boolean): List<JobApplication> =
        applications.sortedWith(compareBy { it.company.lowercase() })
            .let { if (ascending) it else it.reversed() }

    fun sortByStatus(applications: List<JobApplication>, ascending: Boolean): List<JobApplication> =
        applications.sortedWith(compareBy { it.status.name })
            .let { if (ascending) it else it.reversed() }

    fun sortByFit(applications: List<JobApplication>, ascending: Boolean): List<JobApplication> =
        applications.sortedWith(compareBy { -(it.fitEvaluation?.rubricVersion ?: 0) })
            .let { if (ascending) it.reversed() else it }

    fun sortByRole(applications: List<JobApplication>, ascending: Boolean): List<JobApplication> =
        applications.sortedWith(compareBy { it.role?.lowercase() ?: "" })
            .let { if (ascending) it else it.reversed() }
}
