package dev.fnc314.tools.jobtracker.features.artifactpreviewer.domain.di

import dev.fnc314.tools.jobtracker.features.artifactpreviewer.domain.usecase.LoadArtifactContentUseCase
import dev.fnc314.tools.jobtracker.features.artifactpreviewer.domain.usecase.impl.LoadArtifactContentUseCaseImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module declaring domain layer dependencies for the artifact previewer feature.
 */
public val artifactPreviewerDomainModule: Module = module {
    factoryOf(::LoadArtifactContentUseCaseImpl) bind LoadArtifactContentUseCase::class
}
