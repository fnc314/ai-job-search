package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data

import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data.repository.JobApplicationsDetailsRepositoryImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data.source.LocalJobApplicationsDetailsDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data.source.LocalJobApplicationsDetailsDataSourceImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data.source.NetworkJobApplicationsDetailsDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data.source.NetworkJobApplicationsDetailsDataSourceImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data.source.RemoteJobApplicationsDetailsDataSource
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data.source.RemoteJobApplicationsDetailsDataSourceImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.repository.JobApplicationsDetailsRepository
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.GetArtifactSignedUrlUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.LoadCompaniesUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.LoadJobApplicationUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.RecordEventUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.UpdateApplicationStatusUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.UpdateFitScoreUseCase
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
