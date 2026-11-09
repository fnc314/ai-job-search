package dev.fnc314.tools.jobtracker.features.signin.domain.di

import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.GetCurrentUserUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.ObserveAuthStateUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignInUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignOutUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.impl.GetCurrentUserUseCaseImpl
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.impl.ObserveAuthStateUseCaseImpl
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.impl.SignInUseCaseImpl
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.impl.SignOutUseCaseImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module declaring domain use case bindings for the sign-in feature.
 */
public val signInDomainModule: Module = module {
    factoryOf(::SignInUseCaseImpl) bind SignInUseCase::class
    factoryOf(::SignOutUseCaseImpl) bind SignOutUseCase::class
    factoryOf(::ObserveAuthStateUseCaseImpl) bind ObserveAuthStateUseCase::class
    factoryOf(::GetCurrentUserUseCaseImpl) bind GetCurrentUserUseCase::class
}

