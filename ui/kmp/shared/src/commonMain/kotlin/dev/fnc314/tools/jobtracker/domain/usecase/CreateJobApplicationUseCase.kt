package dev.fnc314.tools.jobtracker.domain.usecase

import dev.fnc314.tools.jobtracker.domain.UseCase
import dev.fnc314.tools.jobtracker.data.model.JobApplication

interface CreateJobApplicationUseCase : UseCase<CreateJobApplicationUseCase.Params, String> {
    data class Params(val application: JobApplication)
}
