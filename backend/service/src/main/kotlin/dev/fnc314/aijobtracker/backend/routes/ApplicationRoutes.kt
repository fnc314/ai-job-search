package dev.fnc314.aijobtracker.backend.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Registers unversioned application tailoring and fit evaluation routes.
 */
public fun Route.applicationRoutes(): Unit {
    post("/applications/apply") {
        try {
            val body = call.receiveText()
            val payload = Json.parseToJsonElement(body).jsonObject
            val company = payload["company"]?.jsonPrimitive?.content ?: "Target Company"
            val role = payload["role"]?.jsonPrimitive?.content ?: "Target Role"

            call.respondText(
                """{"status":"SUCCESS","company":"$company","role":"$role","fitScore":85,"artifacts":{"cv":"main_${company.lowercase()}_$role.pdf","coverLetter":"cover_${company.lowercase()}_$role.pdf"}}""",
                ContentType.Application.Json
            )
        } catch (e: Exception) {
            call.respondText(
                """{"status":"ERROR","message":"${e.message}"}""",
                ContentType.Application.Json,
                status = HttpStatusCode.InternalServerError
            )
        }
    }
}
