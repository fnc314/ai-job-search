package dev.fnc314.tools.jobtracker.features.companydetails.domain

import dev.fnc314.tools.jobtracker.core.data.model.CompanyWithApplications
import dev.fnc314.tools.jobtracker.features.companydetails.data.repository.CompanyDetailsRepository

public class LoadCompanyDetailsUseCase(
    private val repository: CompanyDetailsRepository,
) {
    public suspend operator fun invoke(companyId: String): CompanyWithApplications? {
        return repository.getCompanyDetails(companyId)
    }
}
