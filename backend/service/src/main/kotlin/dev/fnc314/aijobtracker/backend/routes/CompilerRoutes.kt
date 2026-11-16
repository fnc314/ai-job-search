package dev.fnc314.aijobtracker.backend.routes

import dev.fnc314.aijobtracker.backend.compilation.DocumentCompilerService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Registers unversioned artifact compilation HTTP routes.
 *
 * @param compilerService Document compiler service instance.
 */
public fun Route.compilerRoutes(compilerService: DocumentCompilerService = DocumentCompilerService()): Unit {
    post("/compiler/compile") {
        try {
            val body = call.receiveText()
            val payload = Json.parseToJsonElement(body).jsonObject
            val latexSource = payload["source"]?.jsonPrimitive?.content ?: ""
            val outputFileName = payload["filename"]?.jsonPrimitive?.content ?: "document.pdf"

            val result = compilerService.compileLatex(latexSource, outputFileName).first()
            call.respondText(
                Json.encodeToString(result),
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
