package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.impl

import dev.fnc314.tools.jobtracker.core.data.model.JobApplication
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.repository.JobApplicationDetailsRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.LoadJobApplicationUseCase

internal class LoadJobApplicationUseCaseImpl(
  private val repository: JobApplicationDetailsRepository,
) : LoadJobApplicationUseCase {
    override suspend fun invoke(params: LoadJobApplicationUseCase.Params): JobApplication? =
        repository.getJobApplication(params.id)
}
