package dev.fnc314.aijobtracker.frontend.features.companydetails.data.repository

import dev.fnc314.aijobtracker.frontend.core.data.model.CompanyWithApplications

/**
 * Repository interface for retrieving employer profiles and their openings.
 */
public fun interface CompanyDetailsRepository {
    /**
     * Retrieves employer metadata and applications for [companyId].
     *
     * @param companyId Canonical employer slug.
     * @return [CompanyWithApplications] or null if not found.
     */
    public suspend operator fun invoke(companyId: String): CompanyWithApplications?
}
