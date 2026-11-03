package dev.fnc314.tools.jobtracker.domain.usecase

import dev.fnc314.tools.jobtracker.domain.UseCase
import dev.fnc314.tools.jobtracker.data.model.JobApplication

interface LoadAllJobApplicationsUseCase : UseCase<LoadAllJobApplicationsUseCase.Params, List<JobApplication>> {
    data class Params(val forceRefresh: Boolean = false)
}
