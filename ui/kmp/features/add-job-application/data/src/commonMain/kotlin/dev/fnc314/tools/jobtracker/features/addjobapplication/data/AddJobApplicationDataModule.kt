package dev.fnc314.tools.jobtracker.features.addjobapplication.data

import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase.CreateJobApplicationUseCase
import dev.fnc314.tools.jobtracker.features.addjobapplication.domain.usecase.CreateLeadUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

public val addJobApplicationDataModule: org.koin.core.module.Module = module {
    factoryOf(::CreateJobApplicationUseCaseImpl) bind CreateJobApplicationUseCase::class
    factoryOf(::CreateLeadUseCaseImpl) bind CreateLeadUseCase::class
}
