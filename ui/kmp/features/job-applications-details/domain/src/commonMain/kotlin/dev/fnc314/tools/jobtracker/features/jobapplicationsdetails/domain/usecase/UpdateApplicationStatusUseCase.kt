package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus

public interface UpdateApplicationStatusUseCase : UseCase<UpdateApplicationStatusUseCase.Params, JobApplication> {
    public data class Params(
        public val id: String,
        public val status: TrackerStatus,
        public val date: String? = null,
        public val note: String? = null,
    )
}
