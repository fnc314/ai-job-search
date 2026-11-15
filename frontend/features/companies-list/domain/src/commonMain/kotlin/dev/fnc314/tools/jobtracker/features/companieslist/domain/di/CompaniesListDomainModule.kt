package dev.fnc314.tools.jobtracker.features.companieslist.domain.di

import dev.fnc314.tools.jobtracker.features.companieslist.domain.FilterCompaniesUseCase
import dev.fnc314.tools.jobtracker.features.companieslist.domain.LoadAllCompaniesUseCase
import dev.fnc314.tools.jobtracker.features.companieslist.domain.SortCompaniesUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/** Koin dependency injection module providing companies list domain use cases. */
public val companiesListDomainModule: Module = module {
    factoryOf(::LoadAllCompaniesUseCase)
    factoryOf(::FilterCompaniesUseCase)
    factoryOf(::SortCompaniesUseCase)
}
