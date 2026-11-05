package dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.core.data.repository.UseCase

public interface LoadAllJobApplicationsUseCase : UseCase<LoadAllJobApplicationsUseCase.Params, List<JobApplication>> {
    public data class Params(public val forceRefresh: Boolean = false)
}
