package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.repository.UseCase
import dev.fnc314.tools.jobtracker.core.data.model.JobApplication

interface LoadJobApplicationUseCase : UseCase<LoadJobApplicationUseCase.Params, JobApplication?> {
    data class Params(val id: String)
}
