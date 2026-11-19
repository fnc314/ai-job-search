package dev.fnc314.aijobtracker.backend.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val artifactJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/**
 * Registers asynchronous application artifact generation and status lookup routes.
 */
public fun Route.artifactRoutes(): Unit {
    post("/artifacts/create") {
        try {
            val body = call.receiveText()
            val payload = if (body.isNotBlank()) artifactJson.parseToJsonElement(body).jsonObject else null
            val applicationId = payload?.get("applicationId")?.jsonPrimitive?.contentOrNull ?: ""
            val company = payload?.get("company")?.jsonPrimitive?.contentOrNull ?: "Target Company"
            val role = payload?.get("role")?.jsonPrimitive?.contentOrNull
                ?: payload?.get("title")?.jsonPrimitive?.contentOrNull
                ?: "Principal Android Engineer"
            val postingText = payload?.get("postingText")?.jsonPrimitive?.contentOrNull
                ?: payload?.get("text")?.jsonPrimitive?.contentOrNull
                ?: ""
            val persist = payload?.get("persist")?.jsonPrimitive?.booleanOrNull ?: true

            val accepted = ApplyPipelineCoordinator.startArtifactGenerationJob(
                applicationId = applicationId,
                company = company,
                role = role,
                postingText = postingText,
                persistToRemote = persist,
            )
            call.respondText(
                artifactJson.encodeToString(accepted),
                ContentType.Application.Json,
                status = HttpStatusCode.Accepted,
            )
        } catch (e: Exception) {
            call.respondText(
                """{"status":"ERROR","message":"${e.message?.replace("\"", "\\\"")}"}""",
                ContentType.Application.Json,
                status = HttpStatusCode.InternalServerError,
            )
        }
    }

    get("/artifacts/jobs/{jobId}") {
        val jobId = call.parameters["jobId"]
        if (jobId.isNullOrBlank()) {
            call.respondText(
                """{"status":"ERROR","message":"Missing jobId"}""",
                ContentType.Application.Json,
                status = HttpStatusCode.BadRequest,
            )
            return@get
        }
        val snapshot = ApplyPipelineCoordinator.getJobSnapshot(jobId)
        if (snapshot == null) {
            call.respondText(
                """{"status":"NOT_FOUND","jobId":"$jobId"}""",
                ContentType.Application.Json,
                status = HttpStatusCode.NotFound,
            )
            return@get
        }
        call.respondText(
            artifactJson.encodeToString(snapshot),
            ContentType.Application.Json,
            status = HttpStatusCode.OK,
        )
    }
}
