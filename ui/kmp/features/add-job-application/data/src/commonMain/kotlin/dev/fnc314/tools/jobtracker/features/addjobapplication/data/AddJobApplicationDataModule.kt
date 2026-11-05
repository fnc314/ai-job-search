package dev.fnc314.tools.jobtracker.features.addjobapplication.data

import dev.fnc314.tools.jobtracker.features.addjobapplication.data.repository.AddJobApplicationRepositoryImpl
import dev.fnc314.tools.jobtracker.features.addjobapplication.data.source.LocalAddJobApplicationDataSource
import dev.fnc314.tools.jobtracker.features.addjobapplication.data.source.LocalAddJobApplicationDataSourceImpl
import dev.fnc314.tools.jobtracker.features.addjobapplication.data.source.RemoteAddJobApplicationDataSource
import dev.fnc314.tools.jobtracker.features.addjobapplication.data.source.RemoteAddJobApplicationDataSourceImpl
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.repository.AddJobApplicationRepository
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase.CreateJobApplicationUseCase
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase.CreateLeadUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val addJobApplicationDataModule: Module = module {
    singleOf(::RemoteAddJobApplicationDataSourceImpl) bind RemoteAddJobApplicationDataSource::class
    singleOf(::LocalAddJobApplicationDataSourceImpl) bind LocalAddJobApplicationDataSource::class
    singleOf(::AddJobApplicationRepositoryImpl) bind AddJobApplicationRepository::class

    factoryOf(::CreateJobApplicationUseCaseImpl) bind CreateJobApplicationUseCase::class
    factoryOf(::CreateLeadUseCaseImpl) bind CreateLeadUseCase::class
}
