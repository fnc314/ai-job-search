package dev.fnc314.tools.jobtracker.features.addjobapplication.data.di

import dev.fnc314.tools.jobtracker.features.addjobapplication.data.repository.impl.AddJobApplicationRepositoryImpl
import dev.fnc314.tools.jobtracker.features.addjobapplication.data.source.LocalAddJobApplicationDataSource
import dev.fnc314.tools.jobtracker.features.addjobapplication.data.source.RemoteAddJobApplicationDataSource
import dev.fnc314.tools.jobtracker.features.addjobapplication.data.source.impl.LocalAddJobApplicationDataSourceImpl
import dev.fnc314.tools.jobtracker.features.addjobapplication.data.source.impl.RemoteAddJobApplicationDataSourceImpl
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.repository.AddJobApplicationRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module declaring data layer dependencies for the add job application feature.
 */
public val addJobApplicationDataModule: Module = module {
    singleOf(::RemoteAddJobApplicationDataSourceImpl) bind RemoteAddJobApplicationDataSource::class
    singleOf(::LocalAddJobApplicationDataSourceImpl) bind LocalAddJobApplicationDataSource::class
    singleOf(::AddJobApplicationRepositoryImpl) bind AddJobApplicationRepository::class
}
