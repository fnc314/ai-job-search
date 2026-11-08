package dev.fnc314.tools.jobtracker.features.addjobapplication.domain.di

import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase.CreateJobApplicationUseCase
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase.CreateLeadUseCase
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase.impl.CreateJobApplicationUseCaseImpl
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase.impl.CreateLeadUseCaseImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module declaring domain use case bindings for the add job application feature.
 */
public val addJobApplicationDomainModule: Module = module {
    factoryOf(::CreateJobApplicationUseCaseImpl) bind CreateJobApplicationUseCase::class
    factoryOf(::CreateLeadUseCaseImpl) bind CreateLeadUseCase::class
}
