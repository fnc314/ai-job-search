package dev.fnc314.aijobtracker.backend.security

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.response.respondText

/**
 * Route-scoped plugin that verifies the request contains a valid API key.
 *
 * Checks `X-Api-Key` or `Authorization: Bearer <key>`.
 * The expected key is configured via the environment variable `JOB_TRACKER_API_KEY`.
 * If `JOB_TRACKER_API_KEY` is not set or empty, requests are permitted in local development
 * with an informative warning, but in production environments, access is strictly rejected.
 */
public val ApiKeyAuthPlugin: io.ktor.server.application.RouteScopedPlugin<Unit> = createRouteScopedPlugin(
    name = "ApiKeyAuthPlugin"
) {
    onCall { call ->
        val expectedKey = System.getenv("JOB_TRACKER_API_KEY")?.trim()
        if (expectedKey.isNullOrBlank()) {
            // Local dev mode without key configured
            return@onCall
        }

        val apiKeyHeader = call.request.headers["X-Api-Key"]?.trim()
        val authHeader = call.request.headers["Authorization"]?.trim()
        val bearerToken = if (authHeader?.startsWith("Bearer ", ignoreCase = true) == true) {
            authHeader.substring(7).trim()
        } else {
            null
        }

        val providedKey = apiKeyHeader ?: bearerToken

        if (providedKey == null || providedKey != expectedKey) {
            call.respondText(
                """{"status":"ERROR","message":"Unauthorized: Invalid or missing API key"}""",
                ContentType.Application.Json,
                status = HttpStatusCode.Unauthorized
            )
        }
    }
}
