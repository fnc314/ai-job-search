package dev.fnc314.tools.jobtracker.features.trackeredits.domain.di

import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.ApproveEditUseCase
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.ExpireStaleEditsUseCase
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.LoadPendingEditsUseCase
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.RejectEditUseCase
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.impl.ApproveEditUseCaseImpl
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.impl.ExpireStaleEditsUseCaseImpl
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.impl.LoadPendingEditsUseCaseImpl
import dev.fnc314.tools.jobtracker.features.trackeredits.domain.usecase.impl.RejectEditUseCaseImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module declaring domain use case bindings for the tracker edits feature.
 */
public val trackerEditsDomainModule: Module = module {
    factoryOf(::ApproveEditUseCaseImpl) bind ApproveEditUseCase::class
    factoryOf(::ExpireStaleEditsUseCaseImpl) bind ExpireStaleEditsUseCase::class
    factoryOf(::LoadPendingEditsUseCaseImpl) bind LoadPendingEditsUseCase::class
    factoryOf(::RejectEditUseCaseImpl) bind RejectEditUseCase::class
}
