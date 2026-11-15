package dev.fnc314.tools.jobtracker.core.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Factory creating platform-specific Ktor [HttpClient] engine.
 */
public expect fun createPlatformHttpClient(): HttpClient

/**
 * Creates and configures a multiplatform JSON-enabled [HttpClient] with content negotiation.
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
    installInspector()
  }
}
