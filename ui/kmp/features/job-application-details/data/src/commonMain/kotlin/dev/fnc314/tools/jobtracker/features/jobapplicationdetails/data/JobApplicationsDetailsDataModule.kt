package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data

import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.repository.JobApplicationsDetailsRepositoryImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.LocalJobApplicationsDetailsDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.LocalJobApplicationsDetailsDataSourceImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.NetworkJobApplicationsDetailsDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.NetworkJobApplicationsDetailsDataSourceImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.RemoteJobApplicationsDetailsDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.data.source.RemoteJobApplicationsDetailsDataSourceImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.repository.JobApplicationsDetailsRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.GetArtifactSignedUrlUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.LoadCompaniesUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.LoadJobApplicationUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.RecordEventUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.UpdateApplicationStatusUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.UpdateFitScoreUseCase
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val jobApplicationsDetailsDataModule: Module = module {
    singleOf(::RemoteJobApplicationsDetailsDataSourceImpl) bind RemoteJobApplicationsDetailsDataSource::class
    singleOf(::NetworkJobApplicationsDetailsDataSourceImpl) bind NetworkJobApplicationsDetailsDataSource::class
    singleOf(::LocalJobApplicationsDetailsDataSourceImpl) bind LocalJobApplicationsDetailsDataSource::class
    singleOf(::JobApplicationsDetailsRepositoryImpl) bind JobApplicationsDetailsRepository::class

    factoryOf(::LoadJobApplicationUseCaseImpl) bind LoadJobApplicationUseCase::class
    factoryOf(::LoadCompaniesUseCaseImpl) bind LoadCompaniesUseCase::class
    factoryOf(::UpdateApplicationStatusUseCaseImpl) bind UpdateApplicationStatusUseCase::class
    factoryOf(::UpdateFitScoreUseCaseImpl) bind UpdateFitScoreUseCase::class
    factoryOf(::RecordEventUseCaseImpl) bind RecordEventUseCase::class
    factoryOf(::GetArtifactSignedUrlUseCaseImpl) bind GetArtifactSignedUrlUseCase::class
}
