package dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import kotlin.time.Instant

/**
 * Use case sorting an in-memory collection of applications by specified column criteria and direction.
 */
public interface SortJobApplicationsUseCase : UseCase<SortJobApplicationsUseCase.Params, List<JobApplication>> {

    /**
     * Supported sortable column attributes.
     *
     * @property displayName Formatted name for UI column headers and sort pickers.
     */
    public enum class SortColumn(public val displayName: String) {
        /** Sort by initial creation timestamp. */
        DATE_CREATED("Date Created"),

        /** Sort by last activity or update timestamp. */
        DATE_UPDATED("Date Updated"),

        /** Alphabetical sort by employer company name. */
        COMPANY("Company"),

        /** Sort by lifecycle status code ordinal. */
        STATUS("Status"),

        /** Numerical sort by fit score rating. */
        FIT("Fit Score"),

        /** Alphabetical sort by position role title. */
        ROLE("Role");

        public companion object {
            /** Default date sort column alias. */
            public val DATE: SortColumn = DATE_CREATED
        }
    }

    /**
     * Execution parameters for sorting applications.
     *
     * @property applications Source list of [JobApplication] records to sort.
     * @property column Selected column attribute [SortColumn].
     * @property ascending True for ascending order, false for descending.
     */
    public data class Params(
        public val applications: List<JobApplication>,
        public val column: SortColumn,
        public val ascending: Boolean,
    ) {
        /**
         * Applies the comparator sorting against [applications].
         *
         * @return Sorted list of [JobApplication] instances.
         */
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
