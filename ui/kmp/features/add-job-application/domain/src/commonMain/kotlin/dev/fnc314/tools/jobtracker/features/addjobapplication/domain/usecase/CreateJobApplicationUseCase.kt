package dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication

interface CreateJobApplicationUseCase : UseCase<CreateJobApplicationUseCase.Params, String> {
    data class Params(val application: JobApplication)
}
