package dev.fnc314.tools.jobtracker.features.companydetails.data.repository

import dev.fnc314.tools.jobtracker.core.data.model.CompanyWithApplications

public interface CompanyDetailsRepository {
    public suspend fun getCompanyDetails(companyId: String): CompanyWithApplications?
}
