package dev.fnc314.tools.jobtracker.domain.usecase

import dev.fnc314.tools.jobtracker.domain.UseCase
import dev.fnc314.tools.jobtracker.data.model.JobApplication
import dev.fnc314.tools.jobtracker.data.model.TrackerStatus

interface UpdateApplicationStatusUseCase : UseCase<UpdateApplicationStatusUseCase.Params, JobApplication> {
    data class Params(
        val id: String,
        val status: TrackerStatus,
        val date: String? = null,
        val note: String? = null,
    )
}
