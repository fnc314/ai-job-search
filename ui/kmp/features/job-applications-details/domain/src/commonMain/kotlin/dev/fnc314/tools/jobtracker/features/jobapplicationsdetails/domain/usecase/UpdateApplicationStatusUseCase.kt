package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.model.TrackerStatus

interface UpdateApplicationStatusUseCase : UseCase<UpdateApplicationStatusUseCase.Params, JobApplication> {
    data class Params(
        val id: String,
        val status: TrackerStatus,
        val date: String? = null,
        val note: String? = null,
    )
}
