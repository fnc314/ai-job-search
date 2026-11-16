package dev.fnc314.aijobtracker.backend.routes

import dev.fnc314.aijobtracker.backend.ranking.RankScorer
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Registers unversioned job ranking HTTP routes.
 */
public fun Route.rankingRoutes(): Unit {
    post("/jobs/rank") {
        try {
            val body = call.receiveText()
            val payload = if (body.isNotBlank()) Json.parseToJsonElement(body).jsonObject else null
            val title = payload?.get("title")?.jsonPrimitive?.content ?: "Software Engineer"
            val company = payload?.get("company")?.jsonPrimitive?.content ?: "Company"
            val text = payload?.get("text")?.jsonPrimitive?.content ?: ""

            val result = RankScorer.evaluate(text)
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
