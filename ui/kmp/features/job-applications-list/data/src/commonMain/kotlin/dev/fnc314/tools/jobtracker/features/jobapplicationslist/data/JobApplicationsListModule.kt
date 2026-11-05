package dev.fnc314.tools.jobtracker.features.jobapplicationslist.data

import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.FilterJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.LoadAllJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.SortJobApplicationsUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val jobApplicationsListDataModule = module {
    factoryOf(::LoadAllJobApplicationsUseCaseImpl) bind LoadAllJobApplicationsUseCase::class
    factory<FilterJobApplicationsUseCase> { FilterJobApplicationsUseCaseImpl() }
    factory<SortJobApplicationsUseCase> { SortJobApplicationsUseCaseImpl() }
}
