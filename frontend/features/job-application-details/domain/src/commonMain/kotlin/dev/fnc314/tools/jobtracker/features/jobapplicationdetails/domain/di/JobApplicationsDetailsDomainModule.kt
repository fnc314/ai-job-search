package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.di

import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.GetArtifactSignedUrlUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.LoadCompaniesUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.LoadJobApplicationUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.RecordEventUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.UpdateApplicationStatusUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.UpdateFitScoreUseCase
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.impl.GetArtifactSignedUrlUseCaseImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.impl.LoadCompaniesUseCaseImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.impl.LoadJobApplicationUseCaseImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.impl.RecordEventUseCaseImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.impl.UpdateApplicationStatusUseCaseImpl
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.domain.usecase.impl.UpdateFitScoreUseCaseImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module declaring domain use case bindings for the job application details feature.
 */
public val jobApplicationsDetailsDomainModule: Module = module {
    factoryOf(::GetArtifactSignedUrlUseCaseImpl) bind GetArtifactSignedUrlUseCase::class
    factoryOf(::LoadCompaniesUseCaseImpl) bind LoadCompaniesUseCase::class
    factoryOf(::LoadJobApplicationUseCaseImpl) bind LoadJobApplicationUseCase::class
    factoryOf(::RecordEventUseCaseImpl) bind RecordEventUseCase::class
    factoryOf(::UpdateApplicationStatusUseCaseImpl) bind UpdateApplicationStatusUseCase::class
    factoryOf(::UpdateFitScoreUseCaseImpl) bind UpdateFitScoreUseCase::class
}
