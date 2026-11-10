package dev.fnc314.tools.jobtracker.features.companydetails.data.di

import dev.fnc314.tools.jobtracker.features.companydetails.data.repository.CompanyDetailsRepository
import dev.fnc314.tools.jobtracker.features.companydetails.data.repository.impl.CompanyDetailsRepositoryImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val companyDetailsDataModule: Module = module {
    singleOf(::CompanyDetailsRepositoryImpl) bind CompanyDetailsRepository::class
}
