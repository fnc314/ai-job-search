package dev.fnc314.tools.jobtracker.features.companydetails.domain.di

import dev.fnc314.tools.jobtracker.features.companydetails.domain.LoadCompanyDetailsUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

public val companyDetailsDomainModule: Module = module {
    factoryOf(::LoadCompanyDetailsUseCase)
}
