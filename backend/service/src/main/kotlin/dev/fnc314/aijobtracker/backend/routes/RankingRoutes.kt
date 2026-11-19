package dev.fnc314.aijobtracker.backend.routes

import dev.fnc314.aijobtracker.backend.ai.client.AiRankingRequest
import dev.fnc314.aijobtracker.backend.ranking.RankScorer
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val rankingJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

private suspend fun handleAiRank(call: ApplicationCall) {
    try {
        val body = call.receiveText()
        val payload = if (body.isNotBlank()) rankingJson.parseToJsonElement(body).jsonObject else null
        val url = payload?.get("url")?.jsonPrimitive?.contentOrNull
        val rawText = payload?.get("postingText")?.jsonPrimitive?.contentOrNull
            ?: payload?.get("text")?.jsonPrimitive?.contentOrNull
        val postingText = ApplyPipelineCoordinator.fetchPostingTextIfUrl(url, rawText)
        val company = payload?.get("company")?.jsonPrimitive?.contentOrNull ?: ""
        val title = payload?.get("title")?.jsonPrimitive?.contentOrNull
            ?: payload?.get("role")?.jsonPrimitive?.contentOrNull
            ?: ""
        val applicationId = payload?.get("applicationId")?.jsonPrimitive?.contentOrNull
        val deterministicScore = payload?.get("deterministicScore")?.jsonPrimitive?.intOrNull ?: 0
        val strengths = payload?.get("strengths")?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
        val gaps = payload?.get("gaps")?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
        val dealBreakers = payload?.get("dealBreakers")?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()

        val response = ApplyPipelineCoordinator.evaluateWithAi(
            request = AiRankingRequest(
                postingText = postingText,
                company = company,
                title = title,
                deterministicScore = deterministicScore,
                strengths = strengths,
                gaps = gaps,
                dealBreakers = dealBreakers,
            ),
            applicationId = applicationId,
        )
        call.respondText(
            rankingJson.encodeToString(response),
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

/**
 * Registers unversioned deterministic and AI-refined job ranking HTTP routes.
 */
public fun Route.rankingRoutes(): Unit {
    post("/jobs/rank") {
        try {
            val body = call.receiveText()
            val payload = if (body.isNotBlank()) rankingJson.parseToJsonElement(body).jsonObject else null
            val text = payload?.get("text")?.jsonPrimitive?.contentOrNull ?: ""

            val result = RankScorer.evaluate(text)
            call.respondText(
                rankingJson.encodeToString(result),
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

    post("/ai/rank") { handleAiRank(call) }
    post("/ai/client/rank") { handleAiRank(call) }
}
