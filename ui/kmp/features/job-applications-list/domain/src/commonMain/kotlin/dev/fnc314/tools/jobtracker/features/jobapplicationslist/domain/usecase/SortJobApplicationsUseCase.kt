package dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import kotlin.time.Instant

public interface SortJobApplicationsUseCase : UseCase<SortJobApplicationsUseCase.Params, List<JobApplication>> {
    public enum class SortColumn(public val displayName: String) {
        DATE_CREATED("Date Created"),
        DATE_UPDATED("Date Updated"),
        COMPANY("Company"),
        STATUS("Status"),
        FIT("Fit Score"),
        ROLE("Role");

        public companion object {
            public val DATE: SortColumn = DATE_CREATED
        }
    }

    public data class Params(
        public val applications: List<JobApplication>,
        public val column: SortColumn,
        public val ascending: Boolean,
    ) {
        public fun sort(): List<JobApplication> {
            val comparator = when (column) {
                SortColumn.DATE_CREATED -> compareBy<JobApplication> { it.createdAt ?: Instant.DISTANT_PAST }
                    .thenBy { it.company }
                SortColumn.DATE_UPDATED -> compareBy<JobApplication> { it.updatedAt ?: it.createdAt ?: Instant.DISTANT_PAST }
                    .thenBy { it.company }
                SortColumn.COMPANY -> compareBy { it.company.lowercase() }
                SortColumn.STATUS -> compareBy { it.status.name }
                SortColumn.FIT -> compareBy { it.fitScore ?: 0 }
                SortColumn.ROLE -> compareBy { it.role?.lowercase() ?: "" }
            }

            val sorted = applications.sortedWith(comparator)
            return if (ascending) sorted else sorted.reversed()
        }
    }
}
