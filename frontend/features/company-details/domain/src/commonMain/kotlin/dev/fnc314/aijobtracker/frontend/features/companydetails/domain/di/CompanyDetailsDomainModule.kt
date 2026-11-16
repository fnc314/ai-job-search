package dev.fnc314.aijobtracker.frontend.features.companydetails.domain.di

import dev.fnc314.aijobtracker.frontend.features.companydetails.domain.LoadCompanyDetailsUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/** Koin dependency injection module providing company details domain use cases. */
public val companyDetailsDomainModule: Module = module {
    factoryOf(::LoadCompanyDetailsUseCase)
}
