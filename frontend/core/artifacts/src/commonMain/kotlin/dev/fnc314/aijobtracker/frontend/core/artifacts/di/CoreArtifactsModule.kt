package dev.fnc314.aijobtracker.frontend.core.artifacts.di

import dev.fnc314.aijobtracker.frontend.core.artifacts.impl.ArtifactsRepositoryImpl
import dev.fnc314.aijobtracker.frontend.core.artifacts.model.ArtifactsRepository
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
