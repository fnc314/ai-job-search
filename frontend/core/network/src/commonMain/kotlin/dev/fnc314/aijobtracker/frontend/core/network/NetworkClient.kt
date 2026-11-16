package dev.fnc314.aijobtracker.frontend.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Factory creating platform-specific Ktor [HttpClient] engine.
 */
public expect fun createPlatformHttpClient(): HttpClient

/**
 * Creates and configures a multiplatform JSON-enabled [HttpClient] with content negotiation
 * and automatic API key authentication when configured in [RemoteNetworkConfig].
 */
public fun createJsonHttpClient(): HttpClient {
  return createPlatformHttpClient().config {
    install(ContentNegotiation) {
      json(
        Json {
          ignoreUnknownKeys = true
          prettyPrint = true
          isLenient = true
        },
      )
    }
    install(DefaultRequest) {
      RemoteNetworkConfig.apiKey?.let { key ->
        if (key.isNotBlank()) {
          header(RemoteNetworkConfig.API_KEY_HEADER, key)
        }
      }
    }
    installInspector()
  }
}
