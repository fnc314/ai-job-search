package dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.di

import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.FilterJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.LoadAllJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.SortJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.impl.FilterJobApplicationsUseCaseImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.impl.LoadAllJobApplicationsUseCaseImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.impl.SortJobApplicationsUseCaseImpl
import org.koin.core.module.Module
import org.koin.dsl.module

public val jobApplicationsListDomainModule: Module = module {
  factory<LoadAllJobApplicationsUseCase> { LoadAllJobApplicationsUseCaseImpl(get()) }
  factory<FilterJobApplicationsUseCase> { FilterJobApplicationsUseCaseImpl() }
  factory<SortJobApplicationsUseCase> { SortJobApplicationsUseCaseImpl() }
}