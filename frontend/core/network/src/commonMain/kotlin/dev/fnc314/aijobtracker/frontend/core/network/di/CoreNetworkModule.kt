package dev.fnc314.aijobtracker.frontend.core.network.di

import dev.fnc314.aijobtracker.frontend.core.network.createJsonHttpClient
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

/** Koin dependency injection module providing the multiplatform HTTP client. */
public val coreNetworkModule: Module = module {
    single<HttpClient> { createJsonHttpClient() }
}
