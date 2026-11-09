package dev.fnc314.tools.jobtracker.features.signin.ui.di

import dev.fnc314.tools.jobtracker.features.signin.data.di.signInDataModule
import dev.fnc314.tools.jobtracker.features.signin.domain.di.signInDomainModule
import dev.fnc314.tools.jobtracker.features.signin.ui.SignInViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module declaring UI layer dependencies for the sign-in feature.
 */
public val signInUiModule: Module = module {
    viewModelOf(::SignInViewModel)
}

/**
 * Aggregate Koin module uniting data, domain, and UI definitions for the sign-in feature.
 */
public val signInFeatureModule: Module = module {
    includes(signInDataModule, signInDomainModule, signInUiModule)
}
