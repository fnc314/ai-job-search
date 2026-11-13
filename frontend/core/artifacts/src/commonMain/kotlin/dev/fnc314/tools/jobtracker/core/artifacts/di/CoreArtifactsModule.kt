package dev.fnc314.tools.jobtracker.core.artifacts.di

import dev.fnc314.tools.jobtracker.core.artifacts.impl.ArtifactsRepositoryImpl
import dev.fnc314.tools.jobtracker.core.artifacts.model.ArtifactsRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module declaring the core artifacts repository binding.
 */
public val coreArtifactsModule: Module = module {
    singleOf(::ArtifactsRepositoryImpl) bind ArtifactsRepository::class
}
