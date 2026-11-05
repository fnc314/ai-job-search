package dev.fnc314.tools.jobtracker.features.jobapplicationslist.data

import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.repository.JobApplicationsListRepositoryImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source.LocalJobApplicationsListDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source.LocalJobApplicationsListDataSourceImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source.RemoteJobApplicationsListDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source.RemoteJobApplicationsListDataSourceImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.repository.JobApplicationsListRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.FilterJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.LoadAllJobApplicationsUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.domain.usecase.SortJobApplicationsUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val jobApplicationsListDataModule: Module = module {
    singleOf(::RemoteJobApplicationsListDataSourceImpl) bind RemoteJobApplicationsListDataSource::class
    singleOf(::LocalJobApplicationsListDataSourceImpl) bind LocalJobApplicationsListDataSource::class
    singleOf(::JobApplicationsListRepositoryImpl) bind JobApplicationsListRepository::class
    factoryOf(::LoadAllJobApplicationsUseCaseImpl) bind LoadAllJobApplicationsUseCase::class
    factory<FilterJobApplicationsUseCase> { FilterJobApplicationsUseCaseImpl() }
    factory<SortJobApplicationsUseCase> { SortJobApplicationsUseCaseImpl() }
}
