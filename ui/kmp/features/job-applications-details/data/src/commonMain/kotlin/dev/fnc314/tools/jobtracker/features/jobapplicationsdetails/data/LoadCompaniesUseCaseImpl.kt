package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data

import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.repository.JobApplicationRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.LoadCompaniesUseCase

internal class LoadCompaniesUseCaseImpl(
    private val repository: JobApplicationRepository,
) : LoadCompaniesUseCase {
    override suspend fun invoke(params: LoadCompaniesUseCase.Params): List<Company> =
        repository.getCompanies()
}
