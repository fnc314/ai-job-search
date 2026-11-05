package dev.fnc314.tools.jobtracker.features.signin.ui

import dev.fnc314.tools.jobtracker.features.signin.data.signInDataModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Koin module declaring UI layer dependencies for authentication and sign-in.
 */
public val signInUiModule: Module = module {
    factoryOf(::SignInViewModel)
}

/**
 * Aggregate Koin module uniting data and UI definitions for the sign-in feature.
 */
public val signInFeatureModule: Module = module {
    includes(signInDataModule, signInUiModule)
}
