package dev.fnc314.aijobtracker.backend.routes

import com.google.firebase.auth.FirebaseAuth
import dev.fnc314.aijobtracker.backend.BackendService
import dev.fnc314.aijobtracker.backend.oauth.AuthExchangeRequest
import dev.fnc314.aijobtracker.backend.oauth.AuthSession
import dev.fnc314.aijobtracker.backend.oauth.OAuthExchangeService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val authJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
public data class AuthErrorResponse(
    public val status: String = "ERROR",
    public val message: String,
)

/**
 * Registers authentication exchange and account sign-in/sign-up endpoints under `/api/auth/sign-in` and `/api/auth/sign-up`.
 */
public fun Route.authRoutes(): Unit {
    post("/api/auth/sign-in") {
        handleAuthExchange(call)
    }

    post("/api/auth/sign-up") {
        handleAuthExchange(call)
    }
}

private suspend fun handleAuthExchange(call: io.ktor.server.application.ApplicationCall) {
    try {
        val bodyText = call.receiveText()
        if (bodyText.isBlank()) {
            call.respondText(
                authJson.encodeToString(AuthErrorResponse(message = "Request body must not be empty")),
                ContentType.Application.Json,
                status = HttpStatusCode.BadRequest,
            )
            return
        }

        val request = authJson.decodeFromString<AuthExchangeRequest>(bodyText)

        BackendService.initFirebaseApp()

        val firebaseAuth = FirebaseAuth.getInstance()
        val session: AuthSession = OAuthExchangeService.exchange(request, firebaseAuth)

        call.respondText(
            authJson.encodeToString(session),
            ContentType.Application.Json,
            status = HttpStatusCode.OK,
        )
    } catch (e: Exception) {
        val errorMsg = e.message ?: "Authentication exchange failed"
        call.respondText(
            authJson.encodeToString(AuthErrorResponse(message = errorMsg)),
            ContentType.Application.Json,
            status = HttpStatusCode.Unauthorized,
        )
    }
}
