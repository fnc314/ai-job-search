package dev.fnc314.tools.jobtracker.features.artifactpreviewer.ui.di

import dev.fnc314.tools.jobtracker.features.artifactpreviewer.data.di.artifactPreviewerDataModule
import dev.fnc314.tools.jobtracker.features.artifactpreviewer.domain.di.artifactPreviewerDomainModule
import dev.fnc314.tools.jobtracker.features.artifactpreviewer.ui.ArtifactPreviewerEntryRegistry
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * Koin module declaring UI layer dependencies for the artifact previewer feature.
 */
public val artifactPreviewerUiModule: Module = module {
    singleOf(::ArtifactPreviewerEntryRegistry)
}

/**
 * Aggregate Koin module uniting data, domain, and UI definitions for the artifact previewer feature.
 */
public val artifactPreviewerFeatureModule: Module = module {
    includes(artifactPreviewerDataModule, artifactPreviewerDomainModule, artifactPreviewerUiModule)
}
