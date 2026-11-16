package dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.domain.usecase.impl

import dev.fnc314.aijobtracker.frontend.core.data.model.Company
import dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.domain.repository.JobApplicationDetailsRepository
import dev.fnc314.aijobtracker.frontend.features.jobapplicationdetails.domain.usecase.LoadCompaniesUseCase

internal class LoadCompaniesUseCaseImpl(
  private val repository: JobApplicationDetailsRepository,
) : LoadCompaniesUseCase {
    override suspend fun invoke(params: LoadCompaniesUseCase.Params): List<Company> =
        repository.getCompanies()
}
