package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.data

import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.domain.usecase.*
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val jobApplicationsDetailsDataModule = module {
    factoryOf(::LoadJobApplicationUseCaseImpl) bind LoadJobApplicationUseCase::class
    factoryOf(::LoadCompaniesUseCaseImpl) bind LoadCompaniesUseCase::class
    factoryOf(::UpdateApplicationStatusUseCaseImpl) bind UpdateApplicationStatusUseCase::class
    factoryOf(::UpdateFitScoreUseCaseImpl) bind UpdateFitScoreUseCase::class
    factoryOf(::RecordEventUseCaseImpl) bind RecordEventUseCase::class
    factoryOf(::GetArtifactSignedUrlUseCaseImpl) bind GetArtifactSignedUrlUseCase::class
}
