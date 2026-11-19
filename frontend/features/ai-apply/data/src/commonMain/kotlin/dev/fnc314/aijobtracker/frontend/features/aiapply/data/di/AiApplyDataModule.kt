package dev.fnc314.aijobtracker.frontend.features.aiapply.data.di

import dev.fnc314.aijobtracker.frontend.features.aiapply.data.repository.impl.ApplyAssistantRepositoryImpl
import dev.fnc314.aijobtracker.frontend.features.aiapply.data.source.LocalApplyAssistantDataSource
import dev.fnc314.aijobtracker.frontend.features.aiapply.data.source.NetworkApplyAssistantDataSource
import dev.fnc314.aijobtracker.frontend.features.aiapply.data.source.RemoteApplyAssistantDataSource
import dev.fnc314.aijobtracker.frontend.features.aiapply.data.source.impl.LocalApplyAssistantDataSourceImpl
import dev.fnc314.aijobtracker.frontend.features.aiapply.data.source.impl.NetworkApplyAssistantDataSourceImpl
import dev.fnc314.aijobtracker.frontend.features.aiapply.data.source.impl.RemoteApplyAssistantDataSourceImpl
import dev.fnc314.aijobtracker.frontend.features.aiapply.domain.repository.ApplyAssistantRepository
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module declaring data layer dependencies for the AI Apply assistant feature.
 */
public val aiApplyDataModule: Module = module {
    singleOf(::LocalApplyAssistantDataSourceImpl) bind LocalApplyAssistantDataSource::class
    single<NetworkApplyAssistantDataSource> { NetworkApplyAssistantDataSourceImpl(getOrNull<HttpClient>()) }
    singleOf(::RemoteApplyAssistantDataSourceImpl) bind RemoteApplyAssistantDataSource::class
    singleOf(::ApplyAssistantRepositoryImpl) bind ApplyAssistantRepository::class
}
