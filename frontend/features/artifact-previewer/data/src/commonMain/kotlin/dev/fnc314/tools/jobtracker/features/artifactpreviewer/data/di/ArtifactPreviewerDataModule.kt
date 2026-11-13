package dev.fnc314.tools.jobtracker.features.artifactpreviewer.data.di

import dev.fnc314.tools.jobtracker.features.artifactpreviewer.data.repository.ArtifactPreviewerRepository
import dev.fnc314.tools.jobtracker.features.artifactpreviewer.data.repository.impl.ArtifactPreviewerRepositoryImpl
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module declaring data layer dependencies for the artifact previewer feature.
 */
public val artifactPreviewerDataModule: Module = module {
    singleOf(::ArtifactPreviewerRepositoryImpl) bind ArtifactPreviewerRepository::class
}
