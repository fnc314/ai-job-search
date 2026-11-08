package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data

import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.repository.JobApplicationsDetailsRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.LoadCompaniesUseCase

public class LoadCompaniesUseCaseImpl(
    private val repository: JobApplicationsDetailsRepository,
) : LoadCompaniesUseCase {
    override suspend fun invoke(params: LoadCompaniesUseCase.Params): List<Company> =
        repository.getCompanies()
}
