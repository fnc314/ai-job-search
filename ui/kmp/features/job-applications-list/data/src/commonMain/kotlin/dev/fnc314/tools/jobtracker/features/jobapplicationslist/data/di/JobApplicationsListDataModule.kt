package dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.di

import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.repository.JobApplicationsListRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.repository.impl.JobApplicationsListRepositoryImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source.LocalJobApplicationsListDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source.RemoteJobApplicationsListDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source.impl.LocalJobApplicationsListDataSourceImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationslist.data.source.impl.RemoteJobApplicationsListDataSourceImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val jobApplicationsListDataModule: Module = module {
    singleOf(::RemoteJobApplicationsListDataSourceImpl) bind RemoteJobApplicationsListDataSource::class
    singleOf(::LocalJobApplicationsListDataSourceImpl) bind LocalJobApplicationsListDataSource::class
    singleOf(::JobApplicationsListRepositoryImpl) bind JobApplicationsListRepository::class
}
