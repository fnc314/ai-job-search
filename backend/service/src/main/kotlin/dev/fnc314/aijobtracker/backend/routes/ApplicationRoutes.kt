package dev.fnc314.aijobtracker.backend.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val routeJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/**
 * Registers unversioned application tailoring and fit evaluation routes.
 */
public fun Route.applicationRoutes(): Unit {
    post("/applications/apply") {
        try {
            val body = call.receiveText()
            val payload = if (body.isNotBlank()) routeJson.parseToJsonElement(body).jsonObject else null
            val url = payload?.get("url")?.jsonPrimitive?.contentOrNull
            val text = payload?.get("text")?.jsonPrimitive?.contentOrNull
            val company = payload?.get("company")?.jsonPrimitive?.contentOrNull
            val title = payload?.get("title")?.jsonPrimitive?.contentOrNull
                ?: payload?.get("role")?.jsonPrimitive?.contentOrNull
            val applicationId = payload?.get("applicationId")?.jsonPrimitive?.contentOrNull
            val persist = payload?.get("persist")?.jsonPrimitive?.booleanOrNull ?: true

            val result = ApplyPipelineCoordinator.evaluateDeterministic(
                url = url,
                rawText = text,
                companyOverride = company,
                roleOverride = title,
                providedApplicationId = applicationId,
                persistToRemote = persist,
            )

            call.respondText(
                routeJson.encodeToString(result),
                ContentType.Application.Json,
            )
        } catch (e: Exception) {
            call.respondText(
                """{"status":"ERROR","message":"${e.message?.replace("\"", "\\\"")}"}""",
                ContentType.Application.Json,
                status = HttpStatusCode.InternalServerError,
            )
        }
    }
}
