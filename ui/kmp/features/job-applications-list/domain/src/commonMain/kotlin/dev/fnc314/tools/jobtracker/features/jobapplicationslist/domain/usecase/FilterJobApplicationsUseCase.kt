package dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * Use case filtering an in-memory collection of applications by lifecycle status and creation age.
 */
public interface FilterJobApplicationsUseCase : UseCase<FilterJobApplicationsUseCase.Params, List<JobApplication>> {

    /**
     * Relative age intervals used for temporal filtering.
     *
     * @property displayName Formatted name for UI dropdown or chip filter.
     */
    public enum class ApplicationAge(public val displayName: String) {
        /** Matches records from any time period. */
        NONE("All Time"),

        /** Created within the last 7 days. */
        LESS_THAN_1_WEEK("Less than 1 Week"),

        /** Created within the last 30 days. */
        LESS_THAN_1_MONTH("Less than 1 Month"),

        /** Created within the last 60 days. */
        LESS_THAN_2_MONTHS("Less than 2 Months"),

        /** Created within the last 90 days. */
        LESS_THAN_3_MONTHS("Less than 3 Months"),

        /** Created within the last 180 days. */
        LESS_THAN_6_MONTHS("Less than 6 Months"),

        /** Created within the last 365 days. */
        LESS_THAN_1_YEAR("Less than 1 Year"),

        /** Created more than 365 days ago. */
        MORE_THAN_1_YEAR("More than 1 Year");

        /**
         * Checks whether a creation timestamp falls within this interval relative to [now].
         *
         * @param createdAt The creation timestamp.
         * @param now Current reference timestamp.
         * @return True if the timestamp satisfies the age criteria.
         */
        public fun matches(createdAt: Instant?, now: Instant): Boolean {
            if (this == NONE) return true
            val date = createdAt ?: return false
            val elapsed = now - date
            return when (this) {
                NONE -> true
                LESS_THAN_1_WEEK -> elapsed < 7.days
                LESS_THAN_1_MONTH -> elapsed < 30.days
                LESS_THAN_2_MONTHS -> elapsed < 60.days
                LESS_THAN_3_MONTHS -> elapsed < 90.days
                LESS_THAN_6_MONTHS -> elapsed < 180.days
                LESS_THAN_1_YEAR -> elapsed < 365.days
                MORE_THAN_1_YEAR -> elapsed >= 365.days
            }
        }
    }

    /**
     * Execution parameters for filtering applications.
     *
     * @property applications Source list of [JobApplication] records to filter.
     * @property status Target lifecycle state filter [TrackerStatus].
     * @property age Relative creation age filter [ApplicationAge].
     */
    public data class Params(
        public val applications: List<JobApplication>,
        public val status: TrackerStatus = TrackerStatus.NONE,
        public val age: ApplicationAge = ApplicationAge.NONE,
    ) {
        /**
         * Applies the status and age criteria against [applications].
         *
         * @param now Current reference timestamp. Defaults to [Clock.System.now].
         * @return Filtered list of [JobApplication] instances.
         */
        public fun filter(now: Instant = Clock.System.now()): List<JobApplication> {
            if (status == TrackerStatus.NONE && age == ApplicationAge.NONE) {
                return applications
            }

            return applications.filter { app ->
                val statusMatches = status == TrackerStatus.NONE ||
                    status == TrackerStatus.UNSPECIFIED ||
                    app.status == status

                val ageMatches = age.matches(app.createdAt, now)

                statusMatches && ageMatches
            }
        }
    }
}
