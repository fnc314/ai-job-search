package dev.fnc314.aijobtracker.frontend.features.companydetails.data.di

import dev.fnc314.aijobtracker.frontend.features.companydetails.data.repository.CompanyDetailsRepository
import dev.fnc314.aijobtracker.frontend.features.companydetails.data.repository.impl.CompanyDetailsRepositoryImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/** Koin dependency injection module providing company details data repositories. */
public val companyDetailsDataModule: Module = module {
    singleOf(::CompanyDetailsRepositoryImpl) bind CompanyDetailsRepository::class
}
