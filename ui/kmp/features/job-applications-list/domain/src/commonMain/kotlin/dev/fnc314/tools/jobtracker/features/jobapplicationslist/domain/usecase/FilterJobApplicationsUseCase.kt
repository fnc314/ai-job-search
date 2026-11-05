package dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

public interface FilterJobApplicationsUseCase : UseCase<FilterJobApplicationsUseCase.Params, List<JobApplication>> {
    public enum class ApplicationAge(public val displayName: String) {
        NONE("All Time"),
        LESS_THAN_1_WEEK("Less than 1 Week"),
        LESS_THAN_1_MONTH("Less than 1 Month"),
        LESS_THAN_2_MONTHS("Less than 2 Months"),
        LESS_THAN_3_MONTHS("Less than 3 Months"),
        LESS_THAN_6_MONTHS("Less than 6 Months"),
        LESS_THAN_1_YEAR("Less than 1 Year"),
        MORE_THAN_1_YEAR("More than 1 Year");

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

    public data class Params(
        public val applications: List<JobApplication>,
        public val status: TrackerStatus = TrackerStatus.NONE,
        public val age: ApplicationAge = ApplicationAge.NONE,
    ) {
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
