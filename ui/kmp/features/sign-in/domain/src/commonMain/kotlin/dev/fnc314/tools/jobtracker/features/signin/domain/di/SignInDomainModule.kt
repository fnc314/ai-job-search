package dev.fnc314.tools.jobtracker.features.signin.domain.di

import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.SignInUseCase
import dev.fnc314.tools.jobtracker.features.signin.domain.usecase.impl.SignInUseCaseImpl
import org.koin.core.module.Module
import org.koin.dsl.*

/**
 * Koin module declaring domain use case bindings for the sign-in feature.
 */
public val signInDomainModule: Module = module {
    factory<SignInUseCase> { SignInUseCaseImpl(get()) }
}
