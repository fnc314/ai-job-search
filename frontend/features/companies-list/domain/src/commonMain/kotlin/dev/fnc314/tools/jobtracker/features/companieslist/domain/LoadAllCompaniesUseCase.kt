package dev.fnc314.tools.jobtracker.features.companieslist.domain

import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.features.companieslist.data.repository.CompaniesListRepository

public class LoadAllCompaniesUseCase(
    private val repository: CompaniesListRepository,
) {
    public suspend operator fun invoke(forceRefresh: Boolean = false): List<Company> {
        return repository(forceRefresh)
    }
}
