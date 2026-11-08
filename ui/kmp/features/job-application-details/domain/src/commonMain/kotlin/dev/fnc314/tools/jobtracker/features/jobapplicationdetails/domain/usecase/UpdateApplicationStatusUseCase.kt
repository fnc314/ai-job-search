package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus
import dev.fnc314.tools.jobtracker.core.data.repository.UseCase

/**
 * Use case orchestrating status transitions, audit event creation, and timestamp updates for an application.
 */
public interface UpdateApplicationStatusUseCase : UseCase<UpdateApplicationStatusUseCase.Params, JobApplication> {

    /**
     * Parameter carrier for application status transitions.
     *
     * @property id Target application slug or UUID identifier.
     * @property status Desired new [TrackerStatus].
     * @property date Optional ISO date string for backdated status changes.
     * @property note Optional explanatory context notes.
     */
    public data class Params(
        public val id: String,
        public val status: TrackerStatus,
        public val date: String? = null,
        public val note: String? = null,
    )
}
