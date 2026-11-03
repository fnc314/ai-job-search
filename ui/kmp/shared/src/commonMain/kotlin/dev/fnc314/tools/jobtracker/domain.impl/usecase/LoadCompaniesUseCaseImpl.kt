package dev.fnc314.tools.jobtracker.domain.impl.usecase

import dev.fnc314.tools.jobtracker.data.model.Company
import dev.fnc314.tools.jobtracker.domain.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.domain.usecase.LoadCompaniesUseCase

internal class LoadCompaniesUseCaseImpl(
    private val repository: JobApplicationRepository,
) : LoadCompaniesUseCase {
    override suspend fun invoke(params: LoadCompaniesUseCase.Params): List<Company> =
        repository.getCompanies()
}
