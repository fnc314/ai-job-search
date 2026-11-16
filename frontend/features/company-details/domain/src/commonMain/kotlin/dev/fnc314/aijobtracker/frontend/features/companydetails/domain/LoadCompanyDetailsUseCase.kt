package dev.fnc314.aijobtracker.frontend.features.companydetails.domain

import dev.fnc314.aijobtracker.frontend.core.data.model.CompanyWithApplications
import dev.fnc314.aijobtracker.frontend.features.companydetails.data.repository.CompanyDetailsRepository

/**
 * Domain usecase that loads detailed company information and associated job applications.
 */
public class LoadCompanyDetailsUseCase(
    private val repository: CompanyDetailsRepository,
) {
    /**
     * Retrieves the company profile and its openings for [companyId].
     *
     * @param companyId Canonical employer slug.
     * @return [CompanyWithApplications] or null when not found.
     */
    public suspend operator fun invoke(companyId: String): CompanyWithApplications? {
        return repository.getCompanyDetails(companyId)
    }
}
