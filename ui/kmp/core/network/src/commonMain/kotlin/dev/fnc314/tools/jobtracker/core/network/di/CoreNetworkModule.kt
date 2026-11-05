package dev.fnc314.tools.jobtracker.core.network.di

import dev.fnc314.tools.jobtracker.core.network.createJsonHttpClient
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

public val coreNetworkModule: Module = module {
    single<HttpClient> { createJsonHttpClient() }
}
