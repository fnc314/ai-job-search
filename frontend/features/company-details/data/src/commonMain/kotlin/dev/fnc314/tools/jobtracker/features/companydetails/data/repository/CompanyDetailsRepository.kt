package dev.fnc314.tools.jobtracker.features.companydetails.data.repository

import dev.fnc314.tools.jobtracker.core.data.model.CompanyWithApplications

/**
 * Repository interface for retrieving employer profiles and their openings.
 */
public interface CompanyDetailsRepository {
    /**
     * Retrieves employer metadata and applications for [companyId].
     *
     * @param companyId Canonical employer slug.
     * @return [CompanyWithApplications] or null if not found.
     */
    public suspend fun getCompanyDetails(companyId: String): CompanyWithApplications?
}
