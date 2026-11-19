package dev.fnc314.aijobtracker.frontend.features.aiapply.ui.di

import dev.fnc314.aijobtracker.frontend.features.aiapply.data.di.aiApplyDataModule
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.di.aiApplyDomainModule
import dev.fnc314.aijobtracker.frontend.features.aiapply.ui.AiApplyEntryRegistry
import dev.fnc314.aijobtracker.frontend.features.aiapply.ui.ApplyAssistantViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Koin module declaring UI layer dependencies for the AI Apply assistant feature.
 */
public val aiApplyUiModule: Module = module {
    viewModelOf(::ApplyAssistantViewModel)
    singleOf(::AiApplyEntryRegistry)
}

/**
 * Aggregate Koin module uniting data, domain, and UI definitions for the AI Apply assistant feature.
 */
public val aiApplyFeatureModule: Module = module {
    includes(aiApplyDataModule, aiApplyDomainModule, aiApplyUiModule)
}
