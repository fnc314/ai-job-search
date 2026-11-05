package dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.repository.UseCase

interface LoadAllJobApplicationsUseCase : UseCase<LoadAllJobApplicationsUseCase.Params, List<JobApplication>> {
    data class Params(val forceRefresh: Boolean = false)
}
